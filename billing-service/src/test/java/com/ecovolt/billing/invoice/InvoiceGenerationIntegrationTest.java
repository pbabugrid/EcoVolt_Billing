package com.ecovolt.billing.invoice;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerRepository;
import com.ecovolt.billing.customer.CustomerStatus;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.meter.MeterStatus;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for invoice generation against a real H2 database.
 * Each test method runs in its own transaction that is rolled back after.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceGenerationIntegrationTest {

    @Autowired InvoiceGenerationService invoiceGenerationService;
    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired MockMvc mockMvc;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-INTTEST")
                .name("Integration Tester")
                .email("inttest@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build());
    }

    // ---------- helper ----------

    private Meter persistMeter(String number) {
        return meterRepository.save(Meter.builder()
                .meterNumber(number)
                .installationDate(LocalDate.of(2023, 1, 1))
                .status(MeterStatus.ACTIVE)
                .customer(customer)
                .build());
    }

    private MeterReading persistReading(Meter meter, LocalDate date, String value) {
        return meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(date)
                .readingValue(new BigDecimal(value))
                .build());
    }

    // ---------- FR1: customer not found ----------

    @Test
    @DisplayName("Unknown customer id throws ResourceNotFoundException (404 semantics)")
    void unknownCustomer_throws404() {
        assertThatThrownBy(() -> invoiceGenerationService.generateForCustomer(999_999L))
                .hasMessageContaining("999999");
    }

    // ---------- FR6: no meter with two readings ----------

    @Test
    @DisplayName("Customer with no meters throws 422")
    void noMeters_throws422() {
        assertThatThrownBy(() -> invoiceGenerationService.generateForCustomer(customer.getId()))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("At least two meter readings");
    }

    @Test
    @DisplayName("Customer with one meter having only one reading throws 422")
    void oneMeterOneReading_throws422() {
        Meter m = persistMeter("MTR-SINGLE");
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00");

        assertThatThrownBy(() -> invoiceGenerationService.generateForCustomer(customer.getId()))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("At least two meter readings");
    }

    // ---------- FR2 + FR3: same-meter pair, correct calculation ----------

    @Test
    @DisplayName("Single meter with two readings produces one correct invoice")
    void singleMeter_twoReadings_createsOneInvoice() {
        Meter m = persistMeter("MTR-BASIC");
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m, LocalDate.of(2024, 2, 1), "160.00");

        List<InvoiceResponse> result = invoiceGenerationService.generateForCustomer(customer.getId());

        assertThat(result).hasSize(1);
        InvoiceResponse inv = result.get(0);
        assertThat(inv.customerId()).isEqualTo(customer.getId());
        assertThat(inv.previousReading()).isEqualByComparingTo("100.00");
        assertThat(inv.currentReading()).isEqualByComparingTo("160.00");
        assertThat(inv.unitsConsumed()).isEqualByComparingTo("60.00");
        assertThat(inv.amount()).isEqualByComparingTo("300.00"); // 60 * 5
        assertThat(inv.status()).isEqualTo(InvoiceStatus.GENERATED);
    }

    // ---------- multi-meter customer (FR2) ----------

    @Test
    @DisplayName("Multi-meter customer generates one invoice per eligible meter")
    void multiMeter_bothEligible_generatesTwoInvoices() {
        Meter m1 = persistMeter("MTR-MULTI-A");
        persistReading(m1, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m1, LocalDate.of(2024, 2, 1), "160.00");

        Meter m2 = persistMeter("MTR-MULTI-B");
        persistReading(m2, LocalDate.of(2024, 1, 1), "200.00");
        persistReading(m2, LocalDate.of(2024, 2, 1), "280.00");

        List<InvoiceResponse> result = invoiceGenerationService.generateForCustomer(customer.getId());

        assertThat(result).hasSize(2);
        // Both invoices belong to the same customer
        assertThat(result).allMatch(r -> r.customerId().equals(customer.getId()));
        // Invoice numbers must be distinct
        assertThat(result.stream().map(InvoiceResponse::invoiceNumber).distinct()).hasSize(2);
    }

    @Test
    @DisplayName("Multi-meter: meter with one reading is skipped; eligible meter generates invoice")
    void multiMeter_oneEligible_oneNot_generatesOneInvoice() {
        Meter eligible = persistMeter("MTR-ELIGIBLE");
        persistReading(eligible, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(eligible, LocalDate.of(2024, 2, 1), "150.00");

        Meter ineligible = persistMeter("MTR-INELIGIBLE");
        persistReading(ineligible, LocalDate.of(2024, 1, 1), "200.00"); // only one reading

        List<InvoiceResponse> result = invoiceGenerationService.generateForCustomer(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).unitsConsumed()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Latest two readings per meter are used (not oldest)")
    void latestTwoReadingsPerMeter_areSelected() {
        Meter m = persistMeter("MTR-ORDER");
        persistReading(m, LocalDate.of(2023, 1, 1), "50.00");  // oldest — must be ignored
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00"); // previous
        persistReading(m, LocalDate.of(2024, 2, 1), "160.00"); // current

        List<InvoiceResponse> result = invoiceGenerationService.generateForCustomer(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previousReading()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).currentReading()).isEqualByComparingTo("160.00");
        assertThat(result.get(0).unitsConsumed()).isEqualByComparingTo("60.00");
    }

    // ---------- FR4 + FR5: duplicate prevention ----------

    @Test
    @DisplayName("Replaying generation after invoicing all pairs throws 422")
    void duplicateReplay_throws422() {
        Meter m = persistMeter("MTR-DUPE");
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m, LocalDate.of(2024, 2, 1), "150.00");

        // First generation — must succeed
        List<InvoiceResponse> first = invoiceGenerationService.generateForCustomer(customer.getId());
        assertThat(first).hasSize(1);

        // Second generation — all pairs are now duplicates → 422
        assertThatThrownBy(() -> invoiceGenerationService.generateForCustomer(customer.getId()))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already been invoiced");
    }

    @Test
    @DisplayName("Multi-meter replay: when one pair is already invoiced, new pair is still created")
    void multiMeterPartialDuplicate_onlyNewPairCreated() {
        Meter m1 = persistMeter("MTR-PDUPE-A");
        persistReading(m1, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m1, LocalDate.of(2024, 2, 1), "150.00");

        Meter m2 = persistMeter("MTR-PDUPE-B");
        persistReading(m2, LocalDate.of(2024, 1, 1), "200.00");
        persistReading(m2, LocalDate.of(2024, 2, 1), "280.00");

        // First call: both meters generate invoices
        List<InvoiceResponse> first = invoiceGenerationService.generateForCustomer(customer.getId());
        assertThat(first).hasSize(2);

        // Add a new reading to m2 only
        persistReading(m2, LocalDate.of(2024, 3, 1), "360.00");

        // Second call: m1 pair is a duplicate (skipped), m2 has a new pair
        List<InvoiceResponse> second = invoiceGenerationService.generateForCustomer(customer.getId());
        assertThat(second).hasSize(1);
        assertThat(second.get(0).previousReading()).isEqualByComparingTo("280.00");
        assertThat(second.get(0).currentReading()).isEqualByComparingTo("360.00");
    }

    @Test
    @DisplayName("Generate endpoint returns 201 with an invoice list")
    void generateEndpoint_returnsCreatedInvoiceList() throws Exception {
        Meter m = persistMeter("MTR-API-LIST");
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m, LocalDate.of(2024, 2, 1), "140.00");

        mockMvc.perform(post("/api/invoices/generate/{customerId}", customer.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId").value(customer.getId()))
                .andExpect(jsonPath("$[0].previousReading").value(100.00))
                .andExpect(jsonPath("$[0].currentReading").value(140.00))
                .andExpect(jsonPath("$[0].unitsConsumed").value(40.00));
    }

    @Test
    @DisplayName("Generate endpoint maps duplicate replay to 422")
    void generateEndpoint_duplicateReplay_returns422() throws Exception {
        Meter m = persistMeter("MTR-API-DUPE");
        persistReading(m, LocalDate.of(2024, 1, 1), "100.00");
        persistReading(m, LocalDate.of(2024, 2, 1), "140.00");

        mockMvc.perform(post("/api/invoices/generate/{customerId}", customer.getId()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/invoices/generate/{customerId}", customer.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "All eligible meter reading pairs for customer id %d have already been invoiced"
                                .formatted(customer.getId())));
    }
}
