package com.ecovolt.dao;

import com.ecovolt.model.CustomerBill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingDAOTest {

    @Mock Connection        connection;
    @Mock PreparedStatement ps;

    // ── helpers ───────────────────────────────────────────────────────────────

    private CustomerBill bill(int customerId) {
        CustomerBill b = new CustomerBill(customerId);
        b.add(LocalDate.of(2026, 3, 1), 2.0, 3.0, 1.0, 29.0);
        return b;
    }

    private List<CustomerBill> bills(int count) {
        List<CustomerBill> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(bill(i));
        return list;
    }

    private static int[] results(int count) {
        int[] r = new int[count];
        Arrays.fill(r, 1);
        return r;
    }

    // ── constructor — no DB calls, no stubs needed ────────────────────────────

    @Test
    void defaultConstructor_usesBatchSizeOf5() {
        assertEquals(5, BillingDAO.BATCH_SIZE);
    }

    @Test
    void customConstructor_invalidBatchSize_throws() {
        assertThrows(IllegalArgumentException.class, () -> new BillingDAO(0));
        assertThrows(IllegalArgumentException.class, () -> new BillingDAO(-1));
    }

    // ── SQL shape ─────────────────────────────────────────────────────────────

    @Test
    void sql_containsInsertIntoBills() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(1));

        new BillingDAO().batchInsertBills(connection, bills(1));

        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("INSERT") && sql.contains("bills")
        ));
    }

    // ── parameter binding ─────────────────────────────────────────────────────

    @Test
    void setsAllEightParameters_forEachBill() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(1));

        new BillingDAO().batchInsertBills(connection, bills(1));

        verify(ps).setInt(   eq(1), eq(1));
        verify(ps).setDate(  eq(2), eq(Date.valueOf("2026-03-01")));
        verify(ps).setDate(  eq(3), eq(Date.valueOf("2026-03-01")));
        verify(ps).setDouble(eq(4), anyDouble()); // off_peak_units
        verify(ps).setDouble(eq(5), anyDouble()); // standard_units
        verify(ps).setDouble(eq(6), anyDouble()); // peak_units
        verify(ps).setDouble(eq(7), anyDouble()); // total_units
        verify(ps).setDouble(eq(8), anyDouble()); // total_amount
    }

    // ── batch flush boundaries (batch size = 5) ───────────────────────────────

    @Test
    void threeBills_belowBatchSize_oneTrailingFlush() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(3));

        new BillingDAO(5).batchInsertBills(connection, bills(3));

        verify(ps, times(1)).executeBatch();
    }

    @Test
    void fiveBills_exactlyOneBatch_oneMidLoopFlush_noTrailingFlush() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(5));

        new BillingDAO(5).batchInsertBills(connection, bills(5));

        verify(ps, times(1)).executeBatch();
    }

    @Test
    void sixBills_oneMidLoopFlushPlusOneTrailingFlush() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch())
                .thenReturn(results(5))
                .thenReturn(results(1));

        new BillingDAO(5).batchInsertBills(connection, bills(6));

        verify(ps, times(2)).executeBatch();
    }

    @Test
    void tenBills_exactlyTwoBatches_twoMidLoopFlushes_noTrailingFlush() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(5));

        new BillingDAO(5).batchInsertBills(connection, bills(10));

        verify(ps, times(2)).executeBatch();
    }

    @Test
    void elevenBills_twoMidLoopFlushesAndOneTrailingFlush() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch())
                .thenReturn(results(5))
                .thenReturn(results(5))
                .thenReturn(results(1));

        new BillingDAO(5).batchInsertBills(connection, bills(11));

        verify(ps, times(3)).executeBatch();
    }

    @Test
    void addBatch_calledOncePerBill() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(5));

        new BillingDAO(5).batchInsertBills(connection, bills(5));

        verify(ps, times(5)).addBatch();
    }

    // ── return value ──────────────────────────────────────────────────────────

    @Test
    void returnsCountOfSuccessfulUpserts() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(results(3));

        int result = new BillingDAO(5).batchInsertBills(connection, bills(3));

        assertEquals(3, result);
    }

    @Test
    void acrossTwoBatches_countsSummedCorrectly() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        // batch 1 flushes 5, batch 2 (trailing) flushes 3 → total 8
        when(ps.executeBatch())
                .thenReturn(results(5))  // mid-loop flush at count=5
                .thenReturn(results(3)); // trailing flush for remaining 3

        int result = new BillingDAO(5).batchInsertBills(connection, bills(8));

        assertEquals(8, result);
    }

    @Test
    void failedRow_notCounted() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        // 3 bills, all go in one trailing flush (batch size 10 > 3)
        // result array: row 1 ok, row 2 failed (EXECUTE_FAILED=-3), row 3 ok → expect 2
        when(ps.executeBatch()).thenReturn(new int[]{1, PreparedStatement.EXECUTE_FAILED, 1});

        int result = new BillingDAO(10).batchInsertBills(connection, bills(3));

        assertEquals(2, result);
    }

    @Test
    void successNoInfo_isCounted() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeBatch()).thenReturn(new int[]{PreparedStatement.SUCCESS_NO_INFO});

        int result = new BillingDAO(5).batchInsertBills(connection, bills(1));

        assertEquals(1, result);
    }

    // ── empty input ───────────────────────────────────────────────────────────

    @Test
    void emptyCollection_returnsZero_noBatchCalls() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);

        int result = new BillingDAO(5).batchInsertBills(connection, List.of());

        assertEquals(0, result);
        verify(ps, never()).addBatch();
        verify(ps, never()).executeBatch();
    }
}
