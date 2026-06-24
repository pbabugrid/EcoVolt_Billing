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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .amount(new BigDecimal("250.00"))
                .generatedDate(LocalDate.of(2024, 2, 1))
                .status(InvoiceStatus.GENERATED)
                .previousReadingRecord(previous)
                .currentReadingRecord(current)
                .build());
        return new TestGraph(customer.getId(), meter.getId(), invoice.getId());
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private record TestGraph(Long customerId, Long meterId, Long invoiceId) {
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
