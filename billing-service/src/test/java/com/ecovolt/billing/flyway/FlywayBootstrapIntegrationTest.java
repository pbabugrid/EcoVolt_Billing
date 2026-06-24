package com.ecovolt.billing.flyway;

import com.ecovolt.billing.customer.CustomerRepository;
import com.ecovolt.billing.customer.CustomerStatus;
import com.ecovolt.billing.invoice.InvoiceGenerationService;
import com.ecovolt.billing.invoice.InvoiceRepository;
import com.ecovolt.billing.invoice.InvoiceStatus;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import com.ecovolt.billing.tariff.TariffPlan;
import com.ecovolt.billing.tariff.TariffPlanRepository;
import com.ecovolt.billing.tariff.TariffType;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Flyway bootstrap integration tests.
 *
 * <p>Not class-level {@code @Transactional}: tests that inspect committed seed data
 * must observe the state Flyway applied at startup. The invoice-generation method
 * is individually {@code @Transactional} so the generated invoice is rolled back
 * and does not pollute subsequent tests or alter seed-data counts.
 *
 * <p>Test sequence (each test is independent):
 * <ol>
 *   <li>Context loads - verifies the application started cleanly.</li>
 *   <li>Migration bookkeeping - 4 applied, 0 pending, no validation errors.</li>
 *   <li>Seeded customers (V4) - 7 customers, all 7 customer numbers present.</li>
 *   <li>Seeded meters (V4) - 7 meters, all 7 meter numbers present.</li>
 *   <li>Seeded tariff plans + slabs (V2) - 3 active plans, 9 slabs, spot-checked rates.</li>
 *   <li>Seeded readings (V4) - 18 readings.</li>
 *   <li>Seeded demo invoices (V4) - 4 invoices, all 4 invoice numbers present.</li>
 *   <li>Repository query - CUST-R001 lookup, MTR-RES-001 top-2 readings.</li>
 *   <li>Invoice generation - CUST-R001 produces 145 units / 815.00 (rolled back).</li>
 * </ol>
 */
@SpringBootTest
class FlywayBootstrapIntegrationTest {

    private static final String DATABASE_NAME =
            "ecovolt_flyway_bootstrap_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired Flyway flyway;
    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired TariffPlanRepository tariffPlanRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired InvoiceGenerationService invoiceGenerationService;

    @DynamicPropertySource
    static void isolatedDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:h2:mem:" + DATABASE_NAME + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    // -----------------------------------------------------------------------
    // 1. Context loads
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Application context loads and Flyway bean is available")
    void contextLoads_flywayBeanPresent() {
        assertThat(flyway).isNotNull();
        assertThat(customerRepository).isNotNull();
        assertThat(invoiceGenerationService).isNotNull();
    }

    // -----------------------------------------------------------------------
    // 2. Migration bookkeeping
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 4 versioned Flyway migrations applied")
    void fourVersionedMigrationsApplied() {
        long versionedApplied = Arrays.stream(flyway.info().applied())
                .filter(m -> m.getVersion() != null)
                .count();
        assertThat(versionedApplied).isEqualTo(4);
    }

    @Test
    @DisplayName("Applied migrations have expected descriptions in order")
    void appliedMigrations_expectedDescriptions() {
        List<String> descriptions = Arrays.stream(flyway.info().applied())
                .filter(m -> m.getVersion() != null)
                .map(MigrationInfo::getDescription)
                .toList();

        assertThat(descriptions).containsExactly(
                "baseline billing schema",
                "seed default tariff plans",
                "add optimistic locking",
                "seed reference and demo data"
        );
    }

    @Test
    @DisplayName("No pending Flyway migrations remain")
    void noPendingMigrations() {
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    @DisplayName("Flyway validate() reports no checksum or state errors")
    void flywayValidation_noErrors() {
        // validate() throws FlywayValidateException on failure; clean execution means no errors
        assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // 3. Seeded customers (V4)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 7 customers seeded by V4")
    void sevenCustomersSeeded() {
        assertThat(customerRepository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("All 7 V4 customer numbers are present in the database")
    void allSeededCustomerNumbersPresent() {
        assertThat(customerRepository.existsByCustomerNumber("CUST-R001")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-R002")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-R003")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-C001")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-C002")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-I001")).isTrue();
        assertThat(customerRepository.existsByCustomerNumber("CUST-I002")).isTrue();
    }

    // -----------------------------------------------------------------------
    // 4. Seeded meters (V4)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 7 meters seeded by V4")
    void sevenMetersSeeded() {
        assertThat(meterRepository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("All 7 V4 meter numbers are present in the database")
    void allSeededMeterNumbersPresent() {
        assertThat(meterRepository.existsByMeterNumber("MTR-RES-001")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-RES-002")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-RES-003")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-COM-001")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-COM-002")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-IND-001")).isTrue();
        assertThat(meterRepository.existsByMeterNumber("MTR-IND-002")).isTrue();
    }

    // -----------------------------------------------------------------------
    // 5. Seeded tariff plans + slabs (V2)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 3 active tariff plans seeded by V2")
    void threeActiveTariffPlansSeeded() {
        long activePlans = tariffPlanRepository.findAll().stream()
                .filter(TariffPlan::isActive)
                .count();
        assertThat(activePlans).isEqualTo(3);
    }

    @Test
    @Transactional
    @DisplayName("Each V2 tariff plan has exactly 3 slabs (9 slabs total) with correct first-slab rates")
    void tariffSlabsSeeded_threePerPlan_correctRates() {
        /*
         * Lazy-loads slabs within a transaction so the persistence context stays open.
         *
         * Expected first-slab rates (sort_order = 1):
         *   RESIDENTIAL -> 5.00 / unit
         *   COMMERCIAL  -> 8.00 / unit
         *   INDUSTRIAL  -> 10.00 / unit
         */
        List<TariffPlan> plans = tariffPlanRepository.findAll();
        assertThat(plans).hasSize(3);
        plans.forEach(plan -> assertThat(plan.getSlabs())
                .as("TariffPlan %s should have 3 slabs", plan.getType())
                .hasSize(3));

        TariffPlan residential = plans.stream()
                .filter(p -> p.getType() == TariffType.RESIDENTIAL).findFirst().orElseThrow();
        TariffPlan commercial = plans.stream()
                .filter(p -> p.getType() == TariffType.COMMERCIAL).findFirst().orElseThrow();
        TariffPlan industrial = plans.stream()
                .filter(p -> p.getType() == TariffType.INDUSTRIAL).findFirst().orElseThrow();

        assertThat(residential.getSlabs().get(0).getRatePerUnit()).isEqualByComparingTo("5.00");
        assertThat(commercial.getSlabs().get(0).getRatePerUnit()).isEqualByComparingTo("8.00");
        assertThat(industrial.getSlabs().get(0).getRatePerUnit()).isEqualByComparingTo("10.00");
    }

    // -----------------------------------------------------------------------
    // 6. Seeded meter readings (V4)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 18 meter readings seeded by V4")
    void eighteenReadingsSeeded() {
        // Ground truth: V4 inserts 18 rows (2+3+3+2+3+3+2 across 7 meters).
        // Discovery doc stated 19; verified as incorrect against the migration SQL.
        assertThat(meterReadingRepository.count()).isEqualTo(18);
    }

    // -----------------------------------------------------------------------
    // 7. Seeded demo invoices (V4)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Exactly 4 demo invoices seeded by V4")
    void fourDemoInvoicesSeeded() {
        assertThat(invoiceRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("All 4 V4 demo invoice numbers are present in the database")
    void allDemoInvoiceNumbersPresent() {
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-DEMO-R002-202403")).isTrue();
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-DEMO-R003-202403")).isTrue();
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-DEMO-C002-202405")).isTrue();
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-DEMO-I001-202403")).isTrue();
    }

    // -----------------------------------------------------------------------
    // 8. Repository queries against seeded data
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CustomerRepository lookup for CUST-R001 returns Alice Greenfield ACTIVE")
    void customerRepository_custR001_returnsAliceGreenfield() {
        var custR001 = customerRepository.findAll().stream()
                .filter(c -> "CUST-R001".equals(c.getCustomerNumber()))
                .findFirst();

        assertThat(custR001).isPresent();
        assertThat(custR001.get().getName()).isEqualTo("Alice Greenfield");
        assertThat(custR001.get().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("MeterReadingRepository top-2 for MTR-RES-001 returns 245.00 then 100.00")
    void meterReadingRepository_top2ForMtrRes001_returnsLatestFirst() {
        var meter = meterRepository.findAll().stream()
                .filter(m -> "MTR-RES-001".equals(m.getMeterNumber()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("MTR-RES-001 not found in database"));

        List<MeterReading> readings =
                meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(meter.getId());

        assertThat(readings).hasSize(2);
        assertThat(readings.get(0).getReadingValue()).isEqualByComparingTo("245.00"); // 2024-03-01
        assertThat(readings.get(1).getReadingValue()).isEqualByComparingTo("100.00"); // 2024-02-01
        assertThat(readings.get(0).getReadingDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(readings.get(1).getReadingDate()).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    @Test
    @DisplayName("TariffPlanRepository findActiveCandidates returns RESIDENTIAL plan for 2024-06-01")
    void tariffPlanRepository_findActiveCandidates_residentialPlan() {
        var candidates = tariffPlanRepository.findActiveCandidates(
                TariffType.RESIDENTIAL, LocalDate.of(2024, 6, 1));

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getVersion()).isEqualTo(1);
        assertThat(candidates.get(0).isActive()).isTrue();
        assertThat(candidates.get(0).getName()).isEqualTo("Residential v1");
    }

    // -----------------------------------------------------------------------
    // 9. Invoice generation using seeded data (CUST-R001)
    //    @Transactional rolls back the generated invoice after assertion.
    // -----------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("Invoice generation for CUST-R001 produces 1 invoice: 145 units, 815.00 (RESIDENTIAL slab calc)")
    void invoiceGeneration_custR001_seededReadings_produces815Amount() {
        /*
         * Scenario - CUST-R001 / MTR-RES-001 (V4 seed):
         *   previous reading: 100.00 kWh  (2024-02-01)
         *   current  reading: 245.00 kWh  (2024-03-01)
         *   units consumed  : 145.00 kWh
         *
         * RESIDENTIAL slab calculation (V2 seed):
         *   first  100 units @ 5.00 =  500.00
         *   next    45 units @ 7.00 =  315.00
         *   total                   =  815.00
         *
         * The generated invoice is rolled back by this method's transaction,
         * so subsequent tests continue to observe exactly 4 seeded demo invoices.
         */
        var custR001 = customerRepository.findAll().stream()
                .filter(c -> "CUST-R001".equals(c.getCustomerNumber()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CUST-R001 not found in database"));

        List<InvoiceResponse> invoices = invoiceGenerationService.generateForCustomer(custR001.getId());

        assertThat(invoices).hasSize(1);
        InvoiceResponse inv = invoices.get(0);
        assertThat(inv.customerId()).isEqualTo(custR001.getId());
        assertThat(inv.previousReading()).isEqualByComparingTo("100.00");
        assertThat(inv.currentReading()).isEqualByComparingTo("245.00");
        assertThat(inv.unitsConsumed()).isEqualByComparingTo("145.00");
        assertThat(inv.amount()).isEqualByComparingTo("815.00");
        assertThat(inv.status()).isEqualTo(InvoiceStatus.GENERATED);
    }
}
