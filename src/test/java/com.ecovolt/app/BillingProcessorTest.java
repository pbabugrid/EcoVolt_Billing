package com.ecovolt.app;

import com.ecovolt.model.CsvRow;
import com.ecovolt.model.CustomerBill;
import com.ecovolt.model.UnitPrice;
import com.ecovolt.strategy.PricingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests BillingProcessor — the pure cost-calculation engine.
 * No DB, no file I/O.
 * <p>
 * Rates used throughout: OFF_PEAK=5, STANDARD=4, PEAK=7
 * So one unit of each = 5+4+7 = 16 per row.
 */
class BillingProcessorTest {

    private BillingProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BillingProcessor(new PricingStrategyFactory(List.of(
                new UnitPrice(1, "OFF_PEAK", 5.0),
                new UnitPrice(2, "STANDARD", 4.0),
                new UnitPrice(3, "PEAK", 7.0)
        )));
    }

    // ── single row ────────────────────────────────────────────────────────────

    @Test
    void singleRow_totalAmountCalculatedCorrectly() {
        // 2*5 + 3*4 + 1*7 = 10+12+7 = 29
        CsvRow row = new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 2.0, 3.0, 1.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(29.0, bill.getTotalAmount(), 0.001);
    }

    @Test
    void singleRow_totalUnitsIsSum() {
        CsvRow row = new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 2.0, 3.0, 1.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(6.0, bill.getTotalUnits(), 0.001);
    }

    @Test
    void singleRow_intervalUnitsStoredSeparately() {
        CsvRow row = new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 1.0, 2.0, 3.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(1.0, bill.getOffPeakUnits(), 0.001);
        assertEquals(2.0, bill.getStandardUnits(), 0.001);
        assertEquals(3.0, bill.getPeakUnits(), 0.001);
    }

    @Test
    void singleRow_customerIdPreserved() {
        CsvRow row = new CsvRow(42, 1, LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(42, bill.getCustomerId());
    }

    @Test
    void singleRow_billingStartAndEndAreSameDate() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        CsvRow row = new CsvRow(1, 1, date, 1.0, 1.0, 1.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(date, bill.getBillingStart());
        assertEquals(date, bill.getBillingEnd());
    }

    // ── accumulation across multiple rows ─────────────────────────────────────

    @Test
    void multipleRowsSameCustomer_amountsAccumulated() {
        List<CsvRow> rows = List.of(
                new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0),
                new CsvRow(1, 1, LocalDate.of(2026, 3, 2), 1.0, 1.0, 1.0)
        );
        // each row = 16, two rows = 32
        CustomerBill bill = processor.process(rows).iterator().next();

        assertEquals(1, processor.process(rows).size());
        assertEquals(32.0, bill.getTotalAmount(), 0.001);
        assertEquals(6.0, bill.getTotalUnits(), 0.001);
    }

    @Test
    void multipleRowsSameCustomer_billingPeriodTracksMinAndMaxDate() {
        List<CsvRow> rows = List.of(
                new CsvRow(1, 1, LocalDate.of(2026, 3, 10), 1.0, 1.0, 1.0),
                new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0),
                new CsvRow(1, 1, LocalDate.of(2026, 3, 20), 1.0, 1.0, 1.0)
        );

        CustomerBill bill = processor.process(rows).iterator().next();

        assertEquals(LocalDate.of(2026, 3, 1), bill.getBillingStart());
        assertEquals(LocalDate.of(2026, 3, 20), bill.getBillingEnd());
    }

    // ── multiple customers ────────────────────────────────────────────────────

    @Test
    void multipleCustomers_eachGetsSeparateBill() {
        List<CsvRow> rows = List.of(
                new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0),
                new CsvRow(2, 2, LocalDate.of(2026, 3, 1), 2.0, 2.0, 2.0)
        );

        Map<Integer, CustomerBill> byCustomer = processor.process(rows).stream()
                .collect(Collectors.toMap(CustomerBill::getCustomerId, b -> b));

        assertEquals(2, byCustomer.size());
        assertEquals(16.0, byCustomer.get(1).getTotalAmount(), 0.001); // 1*16
        assertEquals(32.0, byCustomer.get(2).getTotalAmount(), 0.001); // 2*16
    }

    // ── two meters per customer ───────────────────────────────────────────────

    @Test
    void twoMeters_sameCustomer_mergedIntoOneBill() {
        // meter_id=1 and meter_id=2 both belong to customer_id=1
        List<CsvRow> rows = List.of(
                new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 1.0, 0.0, 0.0),
                new CsvRow(1, 2, LocalDate.of(2026, 3, 1), 0.0, 1.0, 0.0)
        );

        Collection<CustomerBill> bills = processor.process(rows);

        assertEquals(1, bills.size(), "Two meters for same customer must produce ONE bill");
        CustomerBill bill = bills.iterator().next();
        assertEquals(1.0, bill.getOffPeakUnits(), 0.001);
        assertEquals(1.0, bill.getStandardUnits(), 0.001);
        assertEquals(0.0, bill.getPeakUnits(), 0.001);
        assertEquals(9.0, bill.getTotalAmount(), 0.001); // 1*5 + 1*4
    }

    // ── edge cases ────────────────────────────────────────────────────────────

    @Test
    void emptyInput_returnsEmptyCollection() {
        assertTrue(processor.process(List.of()).isEmpty());
    }

    @Test
    void allZeroUnits_totalAmountIsZero() {
        CsvRow row = new CsvRow(1, 1, LocalDate.of(2026, 3, 1), 0.0, 0.0, 0.0);

        CustomerBill bill = processor.process(List.of(row)).iterator().next();

        assertEquals(0.0, bill.getTotalAmount(), 0.001);
        assertEquals(0.0, bill.getTotalUnits(), 0.001);
    }
}
