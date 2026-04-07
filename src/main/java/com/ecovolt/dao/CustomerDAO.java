package com.ecovolt.dao;

import com.ecovolt.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * CRUD for the customers table.
 * Takes Connection as a parameter — no static state, easy to test.
 */
public class CustomerDAO {

    private static final String FIND_BY_ID =
            "SELECT customer_id, name, email FROM customers WHERE customer_id = ?";

    private static final String INSERT =
            "INSERT INTO customers ( name, email) VALUES ( ?, ?)";

    public Optional<Customer> findById(Connection connection, int customerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Customer c = new Customer();
                    c.setCustomerId(rs.getInt("customer_id"));
                    c.setName(rs.getString("name"));
                    c.setEmail(rs.getString("email"));
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    public void insert(Connection connection, Customer customer) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setInt(1, customer.getCustomerId());
            ps.setString(2, customer.getName());
            ps.setString(3, customer.getEmail());
            ps.executeUpdate();
        }
    }

    /**
     * Insert only if customer does not already exist.
     */
    public void upsert(Connection connection, int customerId) throws SQLException {
        if (findById(connection, customerId).isEmpty()) {
            Customer c = new Customer(
                    customerId,
                    "Customer_" + customerId,
                    "customer" + customerId + "@ecovolt.local"
            );
            insert(connection, c);
        }
    }
}
