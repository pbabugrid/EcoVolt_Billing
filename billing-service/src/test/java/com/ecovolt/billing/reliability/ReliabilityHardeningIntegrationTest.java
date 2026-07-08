package com.ecovolt.billing.reliability;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerRepository;
import com.ecovolt.billing.customer.CustomerStatus;
import com.ecovolt.billing.exception.GlobalExceptionHandler;
import com.ecovolt.billing.invoice.Invoice;
import com.ecovolt.billing.invoice.InvoiceRepository;
import com.ecovolt.billing.invoice.InvoiceStatus;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.meter.MeterStatus;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReliabilityHardeningIntegrationTest {

    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("JPA auditing populates timestamps automatically")
    void auditing_populatesTimestamps() {
        Customer saved = transactionTemplate().execute(status -> customerRepository.save(Customer.builder()
                .customerNumber("CUST-AUDIT")
                .name("Audit Tester")
                .email("audit@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build()));

        assertThat(saved).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Customer update preserves createdAt, advances updatedAt, and increments opt_lock")
    void customerUpdate_preservesAuditCreationAndIncrementsVersion() throws Exception {
        CustomerSnapshot original = transactionTemplate().execute(status -> {
            Customer saved = customerRepository.saveAndFlush(Customer.builder()
                    .customerNumber("CUST-AUDIT-MUTATION")
                    .name("Audit Mutation")
                    .email("audit-mutation@ecovolt.test")
                    .status(CustomerStatus.ACTIVE)
                    .build());
            return CustomerSnapshot.from(saved);
        });

        mockMvc.perform(put("/api/customers/{id}", original.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Audit Mutation Updated",
                                  "email": "audit-mutation-updated@ecovolt.test",
                                  "phone": "+15550000001",
                                  "address": "Updated audit address"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id()))
                .andExpect(jsonPath("$.name").value("Audit Mutation Updated"));

        CustomerSnapshot updated = transactionTemplate().execute(status ->
                customerRepository.findById(original.id()).map(CustomerSnapshot::from).orElseThrow());

        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.updatedAt()).isAfter(original.updatedAt());
        assertThat(updated.optLock()).isEqualTo(original.optLock() + 1);
    }

    @Test
    @DisplayName("Optimistic locking rejects stale customer update")
    void optimisticLocking_rejectsStaleUpdate() {
        Long customerId = transactionTemplate().execute(status -> customerRepository.save(Customer.builder()
                .customerNumber("CUST-LOCK")
                .name("Lock Tester")
                .email("lock@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build()).getId());

        Customer stale = loadDetachedCustomer(customerId);

        transactionTemplate().executeWithoutResult(status -> {
            Customer current = customerRepository.findById(customerId).orElseThrow();
            current.setName("Lock Tester Updated");
        });

        stale.setName("Stale Update");
        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> customerRepository.saveAndFlush(stale)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Constraint and optimistic-lock exceptions return 409 ApiError responses")
    void conflictHandlers_returnMeaningfulApiErrors() throws Exception {
        MockMvc standalone = MockMvcBuilders
                .standaloneSetup(new ConflictProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        standalone.perform(get("/probe/data-integrity").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A data constraint was violated. Check unique fields and related resource ids."));

        standalone.perform(get("/probe/optimistic-lock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("The resource was modified by another transaction. Reload and retry."));
    }

    @Test
    @DisplayName("Database uniqueness violations are surfaced as data integrity exceptions")
    void databaseConstraintViolation_raisesDataIntegrityViolation() {
        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
            customerRepository.saveAndFlush(Customer.builder()
                    .customerNumber("CUST-CONSTRAINT")
                    .name("Constraint Tester One")
                    .email("constraint1@ecovolt.test")
                    .status(CustomerStatus.ACTIVE)
                    .build());
            customerRepository.saveAndFlush(Customer.builder()
                    .customerNumber("CUST-CONSTRAINT")
                    .name("Constraint Tester Two")
                    .email("constraint2@ecovolt.test")
                    .status(CustomerStatus.ACTIVE)
                    .build());
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Meter and reading uniqueness constraints reject duplicate persistence")
    void databaseConstraintViolation_rejectsDuplicateMeterNumberAndReadingDate() {
        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
            Customer customer = customerRepository.save(Customer.builder()
                    .customerNumber("CUST-CONSTRAINT-GRAPH")
                    .name("Constraint Graph")
                    .email("constraint-graph@ecovolt.test")
                    .status(CustomerStatus.ACTIVE)
                    .build());
            meterRepository.saveAndFlush(Meter.builder()
                    .meterNumber("MTR-CONSTRAINT-DUP")
                    .installationDate(LocalDate.of(2024, 1, 1))
                    .status(MeterStatus.ACTIVE)
                    .customer(customer)
                    .build());
            meterRepository.saveAndFlush(Meter.builder()
                    .meterNumber("MTR-CONSTRAINT-DUP")
                    .installationDate(LocalDate.of(2024, 1, 2))
                    .status(MeterStatus.ACTIVE)
                    .customer(customer)
                    .build());
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> transactionTemplate().executeWithoutResult(status -> {
            Customer customer = customerRepository.save(Customer.builder()
                    .customerNumber("CUST-READING-CONSTRAINT")
                    .name("Reading Constraint")
                    .email("reading-constraint@ecovolt.test")
                    .status(CustomerStatus.ACTIVE)
                    .build());
            Meter meter = meterRepository.save(Meter.builder()
                    .meterNumber("MTR-READING-CONSTRAINT")
                    .installationDate(LocalDate.of(2024, 1, 1))
                    .status(MeterStatus.ACTIVE)
                    .customer(customer)
                    .build());
            meterReadingRepository.saveAndFlush(MeterReading.builder()
                    .meter(meter)
                    .readingDate(LocalDate.of(2024, 2, 1))
                    .readingValue(new BigDecimal("100.00"))
                    .build());
            meterReadingRepository.saveAndFlush(MeterReading.builder()
                    .meter(meter)
                    .readingDate(LocalDate.of(2024, 2, 1))
                    .readingValue(new BigDecimal("110.00"))
                    .build());
        })).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Paged and customer-scoped APIs expose page metadata and filtered content")
    void pagedAndScopedApis_returnPageResponses() throws Exception {
        TestGraph graph = transactionTemplate().execute(status -> persistGraph("PAGED"));

        mockMvc.perform(get("/api/customers").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(get("/api/meters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/tariff-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/customers/{id}/meters", graph.customerId()).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(graph.meterId()))
                .andExpect(jsonPath("$.content[0].customerId").value(graph.customerId()));

        mockMvc.perform(get("/api/customers/{id}/invoices", graph.customerId()).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(graph.invoiceId()))
                .andExpect(jsonPath("$.content[0].customerId").value(graph.customerId()));

        mockMvc.perform(get("/api/meters/{id}", graph.meterId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(graph.meterId()))
                .andExpect(jsonPath("$.customerId").value(graph.customerId()));

        mockMvc.perform(get("/api/invoices/{id}", graph.invoiceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(graph.invoiceId()))
                .andExpect(jsonPath("$.customerId").value(graph.customerId()));
    }

    @Test
    @DisplayName("Demo seed data reuses V2 tariffs and includes invoices")
    void demoSeedData_reusesV2TariffsAndIncludesInvoices() {
        Long historicalTariffs = jdbcTemplate.queryForObject(
                "select count(*) from tariff_plans where version = 0",
                Long.class);
        Long demoInvoices = jdbcTemplate.queryForObject(
                "select count(*) from invoices where invoice_number like 'INV-DEMO-%'",
                Long.class);
        Long invoicesUsingLatestPairs = jdbcTemplate.queryForObject(
                """
                        with ranked_readings as (
                            select mr.id,
                                   row_number() over (partition by mr.meter_id order by mr.reading_date desc, mr.id desc) as reading_rank
                            from meter_readings mr
                        )
                        select count(*)
                        from invoices i
                        join ranked_readings previous_reading
                          on i.previous_reading_id = previous_reading.id and previous_reading.reading_rank = 2
                        join ranked_readings current_reading
                          on i.current_reading_id = current_reading.id and current_reading.reading_rank = 1
                        where i.invoice_number like 'INV-DEMO-%'
                        """,
                Long.class);

        assertThat(historicalTariffs).isZero();
        assertThat(demoInvoices).isEqualTo(4L);
        assertThat(invoicesUsingLatestPairs).isZero();
        assertDemoInvoiceAmount("INV-DEMO-R002-202403", "710.00");
        assertDemoInvoiceAmount("INV-DEMO-R003-202403", "1130.00");
        assertDemoInvoiceAmount("INV-DEMO-C002-202405", "3040.00");
        assertDemoInvoiceAmount("INV-DEMO-I001-202403", "5650.00");
    }

    @Test
    @DisplayName("Swagger placeholder sort does not break pageable GET APIs")
    void pageableApis_ignoreSwaggerPlaceholderSort() throws Exception {
        TestGraph graph = transactionTemplate().execute(status -> persistGraph("PLACEHOLDER"));
        String placeholderSort = "[\"string\"]";

        mockMvc.perform(get("/api/customers").param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/meters").param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/readings").param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/invoices").param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/tariff-plans").param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/customers/{id}/meters", graph.customerId()).param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/customers/{id}/invoices", graph.customerId()).param("sort", placeholderSort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("QUERY invoice API returns paginated result with Accept-Query header")
    void queryInvoices_returnsPagedResponse() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 1,
                                  "sort": ["string"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Query", "application/json"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("QUERY invoice API applies default pagination from empty JSON body")
    void queryInvoices_appliesDefaultsForEmptyBody() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY-DEFAULTS"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Query", "application/json"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("QUERY invoice API accepts explicit descending sort in JSON body")
    void queryInvoices_acceptsDescendingSort() throws Exception {
        List<Long> ids = transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-SORT-A").invoiceId(),
                persistGraph("QUERY-SORT-B").invoiceId()));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 2,
                                  "sort": ["id,desc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ids.get(1)))
                .andExpect(jsonPath("$.content[1].id").value(ids.get(0)));
    }

    @Test
    @DisplayName("QUERY invoice API validates pagination body")
    void queryInvoices_validatesPaginationBody() throws Exception {
        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": -1,
                                  "size": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.page").exists())
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    @DisplayName("QUERY invoice API rejects unsupported or malformed JSON body")
    void queryInvoices_rejectsUnsupportedOrMalformedBody() throws Exception {
        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("page=0&size=1"))
                .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("QUERY invoice API rejects missing JSON body")
    void queryInvoices_rejectsMissingBody() throws Exception {
        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is malformed or unreadable."));
    }

    @Test
    @DisplayName("QUERY invoice API falls back to id ASC for Swagger placeholder sort")
    void queryInvoices_swaggerSortPlaceholderFallsBackToIdAsc() throws Exception {
        transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-PLACEHOLDER-SORT-A").invoiceId(),
                persistGraph("QUERY-PLACEHOLDER-SORT-B").invoiceId()));
        List<Long> expectedIds = firstInvoiceIdsAsc(2);

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 2,
                                  "sort": ["string"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(expectedIds.get(1)));
    }

    @Test
    @DisplayName("QUERY invoice API returns empty content for out-of-range page")
    void queryInvoices_returnsEmptyContentForOutOfRangePage() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY-OUT-OF-RANGE"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 9999,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(9999))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("QUERY invoice API exposes pagination metadata")
    void queryInvoices_exposesPaginationMetadata() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY-METADATA"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").isBoolean())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("QUERY invoice API reports only page validation errors when page is invalid")
    void queryInvoices_rejectsOnlyNegativePage() throws Exception {
        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": -1,
                                  "size": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.page").exists())
                .andExpect(jsonPath("$.fieldErrors.size").doesNotExist());
    }

    @Test
    @DisplayName("QUERY invoice API reports only size validation errors when size is invalid")
    void queryInvoices_rejectsOnlyZeroSize() throws Exception {
        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.size").exists())
                .andExpect(jsonPath("$.fieldErrors.page").doesNotExist());
    }

    @Test
    @DisplayName("QUERY invoice API applies defaults for explicit null fields")
    void queryInvoices_appliesDefaultsForNullFields() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY-NULL-DEFAULTS"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": null,
                                  "size": null,
                                  "sort": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("QUERY invoice API applies id ASC default sort for empty sort list")
    void queryInvoices_appliesDefaultSortForEmptySortList() throws Exception {
        transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-EMPTY-SORT-A").invoiceId(),
                persistGraph("QUERY-EMPTY-SORT-B").invoiceId()));
        List<Long> expectedIds = firstInvoiceIdsAsc(2);

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 2,
                                  "sort": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(expectedIds.get(1)));
    }

    @Test
    @DisplayName("QUERY invoice API accepts explicit ascending sort")
    void queryInvoices_acceptsAscendingSort() throws Exception {
        transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-SORT-ASC-A").invoiceId(),
                persistGraph("QUERY-SORT-ASC-B").invoiceId()));
        List<Long> expectedIds = firstInvoiceIdsAsc(2);

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 2,
                                  "sort": ["id,asc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(expectedIds.get(1)));
    }

    @Test
    @DisplayName("QUERY invoice API accepts multi-field sort")
    void queryInvoices_acceptsMultiFieldSort() throws Exception {
        List<Long> cancelledIds = transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-MULTI-SORT-CANCELLED-A", InvoiceStatus.CANCELLED, "250.00").invoiceId(),
                persistGraph("QUERY-MULTI-SORT-CANCELLED-B", InvoiceStatus.CANCELLED, "250.00").invoiceId(),
                persistGraph("QUERY-MULTI-SORT-GENERATED", InvoiceStatus.GENERATED, "250.00").invoiceId()));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 3,
                                  "sort": ["status,asc", "id,desc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.content[0].id").value(cancelledIds.get(1)))
                .andExpect(jsonPath("$.content[1].status").value("CANCELLED"))
                .andExpect(jsonPath("$.content[1].id").value(cancelledIds.get(0)))
                .andExpect(jsonPath("$.content[2].status").value("GENERATED"));
    }

    @Test
    @DisplayName("QUERY invoice API returns populated invoice response fields")
    void queryInvoices_returnsPopulatedInvoiceFields() throws Exception {
        TestGraph graph = transactionTemplate().execute(status -> persistGraph("QUERY-FIELDS"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 1,
                                  "sort": ["id,desc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(graph.invoiceId()))
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-QUERY-FIELDS"))
                .andExpect(jsonPath("$.content[0].customerId").value(graph.customerId()))
                .andExpect(jsonPath("$.content[0].amount").value(250.00))
                .andExpect(jsonPath("$.content[0].status").value("GENERATED"));
    }

    @Test
    @DisplayName("QUERY invoice API accepts amount sorting")
    void queryInvoices_acceptsSortByAmount() throws Exception {
        TestGraph highestAmountGraph = transactionTemplate().execute(status -> {
            persistGraph("QUERY-AMOUNT-SORT-LOW", InvoiceStatus.GENERATED, "1.00");
            return persistGraph("QUERY-AMOUNT-SORT-HIGH", InvoiceStatus.GENERATED, "999999.00");
        });

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 1,
                                  "sort": ["amount,desc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(highestAmountGraph.invoiceId()))
                .andExpect(jsonPath("$.content[0].amount").value(999999.00));
    }

    @Test
    @DisplayName("QUERY invoice API skips blank sort entries")
    void queryInvoices_skipsBlankSortEntries() throws Exception {
        transactionTemplate().execute(status -> List.of(
                persistGraph("QUERY-BLANK-SORT-A").invoiceId(),
                persistGraph("QUERY-BLANK-SORT-B").invoiceId()));
        List<Long> expectedIds = firstInvoiceIdsAsc(2);

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 2,
                                  "sort": ["", "id,asc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(expectedIds.get(0)))
                .andExpect(jsonPath("$.content[1].id").value(expectedIds.get(1)));
    }

    @Test
    @DisplayName("GET invoice API with JSON body does not use QUERY handler")
    void getInvoices_withJsonContentType_doesNotSetAcceptQueryHeader() throws Exception {
        transactionTemplate().execute(status -> persistGraph("GET-NOT-QUERY"));

        mockMvc.perform(get("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "size": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Accept-Query"))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("QUERY invoice API applies default page when only size is specified")
    void queryInvoices_appliesDefaultPageForNullPage() throws Exception {
        transactionTemplate().execute(status -> persistGraph("QUERY-DEFAULT-PAGE"));

        mockMvc.perform(request("QUERY", URI.create("/api/invoices"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "size": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("OpenAPI does not expose unsupported QUERY as executable HEAD operation")
    void openApi_doesNotExposeQueryAsHeadOperation() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode invoicePath = objectMapper.readTree(body).path("paths").path("/api/invoices");
        assertThat(invoicePath.has("get")).isTrue();
        assertThat(invoicePath.has("head")).isFalse();
        assertThat(invoicePath.has("requestBody")).isFalse();
    }

    @Test
    @DisplayName("Customer pagination exposes stable sort order and boundary metadata")
    void customerPagination_returnsStableSortOrderAndBoundaryMetadata() throws Exception {
        List<Long> ids = transactionTemplate().execute(status -> List.of(
                customerRepository.save(Customer.builder()
                        .customerNumber("CUST-PAGE-A")
                        .name("Page A")
                        .email("page-a@ecovolt.test")
                        .status(CustomerStatus.ACTIVE)
                        .build()).getId(),
                customerRepository.save(Customer.builder()
                        .customerNumber("CUST-PAGE-B")
                        .name("Page B")
                        .email("page-b@ecovolt.test")
                        .status(CustomerStatus.ACTIVE)
                        .build()).getId(),
                customerRepository.save(Customer.builder()
                        .customerNumber("CUST-PAGE-C")
                        .name("Page C")
                        .email("page-c@ecovolt.test")
                        .status(CustomerStatus.ACTIVE)
                        .build()).getId()));

        mockMvc.perform(get("/api/customers")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ids.get(2)))
                .andExpect(jsonPath("$.content[1].id").value(ids.get(1)))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.last").isBoolean());

        mockMvc.perform(get("/api/customers")
                        .param("page", "9999")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(9999))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    private Customer loadDetachedCustomer(Long customerId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Customer customer = entityManager.find(Customer.class, customerId);
            entityManager.detach(customer);
            return customer;
        } finally {
            entityManager.close();
        }
    }

    private TestGraph persistGraph(String suffix) {
        return persistGraph(suffix, InvoiceStatus.GENERATED, "250.00");
    }

    private TestGraph persistGraph(String suffix, InvoiceStatus invoiceStatus, String amount) {
        Customer customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-" + suffix)
                .name("Scoped Tester")
                .email("scoped@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build());
        Meter meter = meterRepository.save(Meter.builder()
                .meterNumber("MTR-" + suffix)
                .installationDate(LocalDate.of(2024, 1, 1))
                .status(MeterStatus.ACTIVE)
                .customer(customer)
                .build());
        MeterReading previous = meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.of(2024, 1, 1))
                .readingValue(new BigDecimal("100.00"))
                .build());
        MeterReading current = meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.of(2024, 2, 1))
                .readingValue(new BigDecimal("150.00"))
                .build());
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-" + suffix)
                .customer(customer)
                .previousReading(previous.getReadingValue())
                .currentReading(current.getReadingValue())
                .unitsConsumed(new BigDecimal("50.00"))
                .amount(new BigDecimal(amount))
                .generatedDate(LocalDate.of(2024, 2, 1))
                .status(invoiceStatus)
                .previousReadingRecord(previous)
                .currentReadingRecord(current)
                .build());
        return new TestGraph(customer.getId(), meter.getId(), invoice.getId());
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private void assertDemoInvoiceAmount(String invoiceNumber, String expectedAmount) {
        BigDecimal amount = jdbcTemplate.queryForObject(
                "select amount from invoices where invoice_number = ?",
                BigDecimal.class,
                invoiceNumber);
        assertThat(amount).isEqualByComparingTo(expectedAmount);
    }

    private List<Long> firstInvoiceIdsAsc(int limit) {
        return jdbcTemplate.queryForList("select id from invoices order by id asc limit ?", Long.class, limit);
    }

    private record TestGraph(Long customerId, Long meterId, Long invoiceId) {
    }

    private record CustomerSnapshot(Long id, Instant createdAt, Instant updatedAt, Long optLock) {
        private static CustomerSnapshot from(Customer customer) {
            return new CustomerSnapshot(
                    customer.getId(),
                    customer.getCreatedAt(),
                    customer.getUpdatedAt(),
                    customer.getOptLock());
        }
    }

    @RestController
    private static class ConflictProbeController {
        @GetMapping("/probe/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("duplicate key value violates constraint uk_probe");
        }

        @GetMapping("/probe/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(Customer.class, 1L);
        }
    }
}
