package com.ecovolt.dao;

import com.ecovolt.model.Meter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeterDAOTest {

    @Mock Connection        connection;
    @Mock PreparedStatement ps;
    @Mock ResultSet         rs;

    private final MeterDAO dao = new MeterDAO();

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_meterExists_returnsPopulatedOptional() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("meter_id")).thenReturn(1);
        when(rs.getInt("customer_id")).thenReturn(5);
        when(rs.getString("meter_number")).thenReturn("MTR-1");

        Optional<Meter> result = dao.findById(connection, 1);

        assertTrue(result.isPresent());
        assertEquals(1,       result.get().getMeterId());
        assertEquals(5,       result.get().getCustomerId());
        assertEquals("MTR-1", result.get().getMeterNumber());
    }

    @Test
    void findById_meterNotFound_returnsEmptyOptional() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findById(connection, 99).isEmpty());
    }

    @Test
    void findById_setsMeterIdParameter() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.findById(connection, 42);

        verify(ps).setInt(1, 42);
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insert_setsAllThreeParameters() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        Meter m = new Meter(10, 3, "MTR-10");

        dao.insert(connection, m);

        verify(ps).setInt(1, 10);
        verify(ps).setInt(2, 3);
        verify(ps).setString(3, "MTR-10");
        verify(ps).executeUpdate();
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    void upsert_meterNotExists_insertsNewMeter() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.upsert(connection, 5, 1);

        verify(connection, times(2)).prepareStatement(anyString());
        verify(ps).executeUpdate();
    }

    @Test
    void upsert_meterNotExists_meterNumberIsFormatted() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.upsert(connection, 7, 2);

        verify(ps).setString(eq(3), eq("MTR-7"));
    }

    @Test
    void upsert_meterExistsSameCustomer_doesNotInsert() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("meter_id")).thenReturn(5);
        when(rs.getInt("customer_id")).thenReturn(1);
        when(rs.getString("meter_number")).thenReturn("MTR-5");

        dao.upsert(connection, 5, 1);

        verify(connection, times(1)).prepareStatement(anyString());
        verify(ps, never()).executeUpdate();
    }

    @Test
    void upsert_meterExistsDifferentCustomer_throwsIllegalArgumentException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("meter_id")).thenReturn(5);
        when(rs.getInt("customer_id")).thenReturn(99);
        when(rs.getString("meter_number")).thenReturn("MTR-5");

        assertThrows(IllegalArgumentException.class, () -> dao.upsert(connection, 5, 1));
    }

    @Test
    void upsert_exceptionMessage_containsMeterAndCustomerIds() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("meter_id")).thenReturn(5);
        when(rs.getInt("customer_id")).thenReturn(99);
        when(rs.getString("meter_number")).thenReturn("MTR-5");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dao.upsert(connection, 5, 1));

        assertTrue(ex.getMessage().contains("5"));
        assertTrue(ex.getMessage().contains("99"));
        assertTrue(ex.getMessage().contains("1"));
    }

    // ── model getters — no mocks needed ──────────────────────────────────────

    @Test
    void meter_gettersReturnConstructedValues() {
        Meter m = new Meter(3, 7, "MTR-3");

        assertEquals(3,       m.getMeterId());
        assertEquals(7,       m.getCustomerId());
        assertEquals("MTR-3", m.getMeterNumber());
    }

    @Test
    void meter_noArgConstructor_settersWork() {
        Meter m = new Meter();
        m.setMeterId(8);
        m.setCustomerId(2);
        m.setMeterNumber("MTR-8");

        assertEquals(8,       m.getMeterId());
        assertEquals(2,       m.getCustomerId());
        assertEquals("MTR-8", m.getMeterNumber());
    }
}
