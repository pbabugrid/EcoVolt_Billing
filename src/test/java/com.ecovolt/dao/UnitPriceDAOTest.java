package com.ecovolt.dao;

import com.ecovolt.model.UnitPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitPriceDAOTest {

    @Mock Connection        connection;
    @Mock PreparedStatement ps;
    @Mock ResultSet         rs;

    private final UnitPriceDAO dao = new UnitPriceDAO();

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllThreeRows() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, true, false);
        when(rs.getInt("price_id")).thenReturn(1, 2, 3);
        when(rs.getString("interval_type")).thenReturn("OFF_PEAK", "STANDARD", "PEAK");
        when(rs.getDouble("price_per_unit")).thenReturn(5.0, 4.0, 7.0);

        assertEquals(3, dao.findAll(connection).size());
    }

    @Test
    void findAll_mapsFieldsCorrectly() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getInt("price_id")).thenReturn(2);
        when(rs.getString("interval_type")).thenReturn("PEAK");
        when(rs.getDouble("price_per_unit")).thenReturn(9.0);

        UnitPrice price = dao.findAll(connection).get(0);

        assertEquals(2,      price.getPriceId());
        assertEquals("PEAK", price.getIntervalType());
        assertEquals(9.0,    price.getPricePerUnit(), 0.001);
    }

    @Test
    void findAll_emptyTable_returnsEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findAll(connection).isEmpty());
    }

    @Test
    void findAll_usesSelectAllQuery() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.findAll(connection);

        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("SELECT") && sql.contains("unit_prices")
        ));
    }

    // ── model getters — no mocks needed ──────────────────────────────────────

    @Test
    void unitPrice_gettersReturnConstructedValues() {
        UnitPrice up = new UnitPrice(1, "PEAK", 7.0);

        assertEquals(1,      up.getPriceId());
        assertEquals("PEAK", up.getIntervalType());
        assertEquals(7.0,    up.getPricePerUnit(), 0.001);
    }

    @Test
    void unitPrice_noArgConstructor_settersWork() {
        UnitPrice up = new UnitPrice();
        up.setPriceId(3);
        up.setIntervalType("STANDARD");
        up.setPricePerUnit(4.5);

        assertEquals(3,          up.getPriceId());
        assertEquals("STANDARD", up.getIntervalType());
        assertEquals(4.5,        up.getPricePerUnit(), 0.001);
    }
}
