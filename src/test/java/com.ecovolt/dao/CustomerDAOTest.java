package com.ecovolt.dao;

import com.ecovolt.model.Customer;
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
class CustomerDAOTest {

    @Mock Connection        connection;
    @Mock PreparedStatement ps;
    @Mock ResultSet         rs;

    private final CustomerDAO dao = new CustomerDAO();

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_customerExists_returnsPopulatedOptional() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("customer_id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Customer_1");
        when(rs.getString("email")).thenReturn("customer1@ecovolt.local");

        Optional<Customer> result = dao.findById(connection, 1);

        assertTrue(result.isPresent());
        assertEquals(1,                         result.get().getCustomerId());
        assertEquals("Customer_1",              result.get().getName());
        assertEquals("customer1@ecovolt.local", result.get().getEmail());
    }

    @Test
    void findById_customerNotFound_returnsEmptyOptional() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findById(connection, 99).isEmpty());
    }

    @Test
    void findById_setsCustomerIdParameter() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.findById(connection, 5);

        verify(ps).setInt(1, 5);
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insert_setsAllThreeParameters() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        Customer c = new Customer(3, "Alice", "alice@ecovolt.local");

        dao.insert(connection, c);

        verify(ps).setInt(1, 3);
        verify(ps).setString(2, "Alice");
        verify(ps).setString(3, "alice@ecovolt.local");
        verify(ps).executeUpdate();
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    void upsert_customerNotExists_insertsNewCustomer() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.upsert(connection, 10);

        verify(connection, times(2)).prepareStatement(anyString());
        verify(ps).executeUpdate();
    }

    @Test
    void upsert_customerAlreadyExists_doesNotInsert() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("customer_id")).thenReturn(10);
        when(rs.getString("name")).thenReturn("Customer_10");
        when(rs.getString("email")).thenReturn("customer10@ecovolt.local");

        dao.upsert(connection, 10);

        verify(connection, times(1)).prepareStatement(anyString());
        verify(ps, never()).executeUpdate();
    }

    @Test
    void upsert_generatesCorrectDefaultNameAndEmail() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.upsert(connection, 7);

        verify(ps).setString(eq(2), eq("Customer_7"));
        verify(ps).setString(eq(3), eq("customer7@ecovolt.local"));
    }

    // ── model getters — no mocks needed ──────────────────────────────────────

    @Test
    void customer_gettersReturnConstructedValues() {
        Customer c = new Customer(5, "Bob", "bob@test.com");

        assertEquals(5,             c.getCustomerId());
        assertEquals("Bob",         c.getName());
        assertEquals("bob@test.com", c.getEmail());
    }

    @Test
    void customer_noArgConstructor_settersWork() {
        Customer c = new Customer();
        c.setCustomerId(9);
        c.setName("Carol");
        c.setEmail("carol@test.com");

        assertEquals(9,               c.getCustomerId());
        assertEquals("Carol",         c.getName());
        assertEquals("carol@test.com", c.getEmail());
    }
}
