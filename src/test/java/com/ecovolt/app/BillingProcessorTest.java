package com.ecovolt.app;

import com.ecovolt.model.CsvRow;
import com.ecovolt.model.CustomerBill;
import com.ecovolt.strategy.PricingStrategy;
import com.ecovolt.strategy.PricingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the pure BillingProcessor.
 * Follows the project standards established in pom.xml:
 * - JUnit Jupiter 5
 * - Mockito for layer isolation
 */
class BillingProcessorTest {

    private PricingStrategy mockOffPeak;
    private PricingStrategy mockStandard;
    private PricingStrategy mockPeak;
    private BillingProcessor processor;

    @BeforeEach
    void setUp() {
        PricingStrategyFactory mockFactory = mock(PricingStrategyFactory.class);
        mockOffPeak = mock(PricingStrategy.class);
        mockStandard = mock(PricingStrategy.class);
        mockPeak = mock(PricingStrategy.class);

        when(mockFactory.offPeak()).thenReturn(mockOffPeak);
        when(mockFactory.standard()).thenReturn(mockStandard);
        when(mockFactory.peak()).thenReturn(mockPeak);

        processor = new BillingProcessor(mockFactory);
    }

    @Test
    void testProcess_WithSingleRow() {
        CsvRow row = new CsvRow(1, 101, LocalDate.of(2026, 4, 1), 10.0, 20.0, 30.0);

        when(mockOffPeak.calculateCost(10.0)).thenReturn(50.0);
        when(mockStandard.calculateCost(20.0)).thenReturn(80.0);
        when(mockPeak.calculateCost(30.0)).thenReturn(210.0);

        Collection<CustomerBill> bills = processor.process(List.of(row));

        assertEquals(1, bills.size(), "Should produce exactly one bill");
        CustomerBill bill = bills.iterator().next();

        assertEquals(1, bill.getCustomerId());
        assertEquals(340.0, bill.getTotalAmount(), "Total should be sum of strategy costs (50 + 80 + 210)");
        assertEquals(60.0, bill.getTotalUnits(), "Total units should be 10 + 20 + 30");
        assertEquals(LocalDate.of(2026, 4, 1), bill.getBillingStart());
        assertEquals(LocalDate.of(2026, 4, 1), bill.getBillingEnd());
    }

    @Test
    void testProcess_CombinesMultipleRowsForSameCustomer() {
        CsvRow row1 = new CsvRow(1, 101, LocalDate.of(2026, 4, 1), 10.0, 0.0, 0.0);
        CsvRow row2 = new CsvRow(1, 102, LocalDate.of(2026, 4, 15), 0.0, 20.0, 0.0);

        when(mockOffPeak.calculateCost(10.0)).thenReturn(50.0);
        when(mockOffPeak.calculateCost(0.0)).thenReturn(0.0);
        when(mockStandard.calculateCost(20.0)).thenReturn(80.0);
        when(mockStandard.calculateCost(0.0)).thenReturn(0.0);
        when(mockPeak.calculateCost(0.0)).thenReturn(0.0);

        Collection<CustomerBill> bills = processor.process(List.of(row1, row2));

        assertEquals(1, bills.size(), "Should aggregate into exactly one bill for customer 1");
        CustomerBill bill = bills.iterator().next();

        assertEquals(1, bill.getCustomerId());
        assertEquals(130.0, bill.getTotalAmount(), "Total should be sum of all strategy costs: 50 + 80");
        assertEquals(30.0, bill.getTotalUnits(), "Total units should be 10 + 20");
        assertEquals(LocalDate.of(2026, 4, 1), bill.getBillingStart());
        assertEquals(LocalDate.of(2026, 4, 15), bill.getBillingEnd());
    }

    @Test
    void testProcess_HandlesMultipleCustomers() {
        CsvRow cust1Row = new CsvRow(1, 101, LocalDate.of(2026, 4, 1), 10.0, 0.0, 0.0);
        CsvRow cust2Row = new CsvRow(2, 201, LocalDate.of(2026, 4, 2), 0.0, 10.0, 0.0);

        when(mockOffPeak.calculateCost(10.0)).thenReturn(50.0);
        when(mockStandard.calculateCost(0.0)).thenReturn(0.0);
        when(mockPeak.calculateCost(0.0)).thenReturn(0.0);

        when(mockOffPeak.calculateCost(0.0)).thenReturn(0.0);
        when(mockStandard.calculateCost(10.0)).thenReturn(40.0);

        Collection<CustomerBill> bills = processor.process(List.of(cust1Row, cust2Row));

        assertEquals(2, bills.size(), "Should produce exactly two bills, one per customer");

        List<CustomerBill> billList = List.copyOf(bills);
        assertEquals(1, billList.get(0).getCustomerId());
        assertEquals(50.0, billList.get(0).getTotalAmount());

        assertEquals(2, billList.get(1).getCustomerId());
        assertEquals(40.0, billList.get(1).getTotalAmount());
    }

    @Test
    void testProcess_EmptyList() {
        Collection<CustomerBill> bills = processor.process(List.of());
        assertTrue(bills.isEmpty(), "Processing an empty list should return an empty collection");
    }

    @Test
    void testProcess_NullInput_ThrowsNullPointerException() {
        Collection<CustomerBill> bills = processor.process(null);
        assertTrue(bills.isEmpty(), "Processing a null list should return an empty collection");
    }

    @Test
    void testProcess_UnorderedDates_ResolvesBillingPeriodCorrectly() {
        // Rows arrive out of chronological order
        CsvRow rowMiddle = new CsvRow(1, 101, LocalDate.of(2026, 4, 15), 10.0, 0.0, 0.0);
        CsvRow rowEnd = new CsvRow(1, 101, LocalDate.of(2026, 4, 30), 10.0, 0.0, 0.0);
        CsvRow rowStart = new CsvRow(1, 101, LocalDate.of(2026, 4, 1), 10.0, 0.0, 0.0);

        when(mockOffPeak.calculateCost(10.0)).thenReturn(50.0);
        when(mockStandard.calculateCost(0.0)).thenReturn(0.0);
        when(mockPeak.calculateCost(0.0)).thenReturn(0.0);

        Collection<CustomerBill> bills = processor.process(List.of(rowMiddle, rowEnd, rowStart));

        assertEquals(1, bills.size());
        CustomerBill bill = bills.iterator().next();

        assertEquals(LocalDate.of(2026, 4, 1), bill.getBillingStart(), "Billing start should be the earliest date");
        assertEquals(LocalDate.of(2026, 4, 30), bill.getBillingEnd(), "Billing end should be the latest date");
    }
}
