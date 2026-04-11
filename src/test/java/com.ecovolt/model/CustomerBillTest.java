package com.ecovolt.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests CustomerBill accumulation logic directly — no strategies or DAOs needed.
 */
class CustomerBillTest {

    @Test
    void newBill_allValuesStartAtZeroOrNull() {
        CustomerBill bill = new CustomerBill(1);

        assertEquals(1, bill.getCustomerId());
        assertEquals(0.0, bill.getOffPeakUnits(), 0.001);
        assertEquals(0.0, bill.getStandardUnits(), 0.001);
        assertEquals(0.0, bill.getPeakUnits(), 0.001);
        assertEquals(0.0, bill.getTotalUnits(), 0.001);
        assertEquals(0.0, bill.getTotalAmount(), 0.001);
        assertNull(bill.getBillingStart());
        assertNull(bill.getBillingEnd());
    }

    @Test
    void add_singleEntry_allFieldsPopulated() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 1), 1.0, 2.0, 3.0, 29.0);

        assertEquals(1.0, bill.getOffPeakUnits(), 0.001);
        assertEquals(2.0, bill.getStandardUnits(), 0.001);
        assertEquals(3.0, bill.getPeakUnits(), 0.001);
        assertEquals(6.0, bill.getTotalUnits(), 0.001);
        assertEquals(29.0, bill.getTotalAmount(), 0.001);
    }

    @Test
    void add_twoEntries_unitsAndAmountsAccumulate() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0, 16.0);
        bill.add(LocalDate.of(2026, 3, 2), 2.0, 2.0, 2.0, 32.0);

        assertEquals(3.0, bill.getOffPeakUnits(), 0.001);
        assertEquals(3.0, bill.getStandardUnits(), 0.001);
        assertEquals(3.0, bill.getPeakUnits(), 0.001);
        assertEquals(9.0, bill.getTotalUnits(), 0.001);
        assertEquals(48.0, bill.getTotalAmount(), 0.001);
    }

    @Test
    void add_firstEntry_billingStartAndEndSetToSameDate() {
        CustomerBill bill = new CustomerBill(1);
        LocalDate d = LocalDate.of(2026, 3, 10);
        bill.add(d, 1.0, 1.0, 1.0, 16.0);

        assertEquals(d, bill.getBillingStart());
        assertEquals(d, bill.getBillingEnd());
    }

    @Test
    void add_earlierDate_updatesBillingStart() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 15), 1.0, 1.0, 1.0, 16.0);
        bill.add(LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0, 16.0);

        assertEquals(LocalDate.of(2026, 3, 1), bill.getBillingStart());
    }

    @Test
    void add_laterDate_updatesBillingEnd() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0, 16.0);
        bill.add(LocalDate.of(2026, 3, 20), 1.0, 1.0, 1.0, 16.0);

        assertEquals(LocalDate.of(2026, 3, 20), bill.getBillingEnd());
    }

    @Test
    void add_dateBetweenStartAndEnd_doesNotChangePeriod() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 1), 1.0, 1.0, 1.0, 16.0);
        bill.add(LocalDate.of(2026, 3, 31), 1.0, 1.0, 1.0, 16.0);
        bill.add(LocalDate.of(2026, 3, 15), 1.0, 1.0, 1.0, 16.0); // middle

        assertEquals(LocalDate.of(2026, 3, 1), bill.getBillingStart());
        assertEquals(LocalDate.of(2026, 3, 31), bill.getBillingEnd());
    }

    @Test
    void add_zeroUnits_totalRemainsZero() {
        CustomerBill bill = new CustomerBill(1);
        bill.add(LocalDate.of(2026, 3, 1), 0.0, 0.0, 0.0, 0.0);

        assertEquals(0.0, bill.getTotalUnits(), 0.001);
        assertEquals(0.0, bill.getTotalAmount(), 0.001);
    }
}
