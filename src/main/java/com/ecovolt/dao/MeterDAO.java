package com.ecovolt.dao;

import com.ecovolt.model.Meter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * CRUD for the meters table.
 * A customer can have 1 or 2 meters.
 * Takes Connection as a parameter — no static state, easy to test.
 */
public class MeterDAO {

    private static final String FIND_BY_ID =
            "SELECT meter_id, customer_id, meter_number FROM meters WHERE meter_id = ?";

    private static final String INSERT =
            "INSERT INTO meters (meter_id, customer_id, meter_number) VALUES (?, ?, ?)";

    public Optional<Meter> findById(Connection connection, int meterId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, meterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Meter m = new Meter();
                    m.setMeterId(rs.getInt("meter_id"));
                    m.setCustomerId(rs.getInt("customer_id"));
                    m.setMeterNumber(rs.getString("meter_number"));
                    return Optional.of(m);
                }
            }
        }
        return Optional.empty();
    }

    public void insert(Connection connection, Meter meter) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(INSERT)) {
            ps.setInt(1, meter.getMeterId());
            ps.setInt(2, meter.getCustomerId());
            ps.setString(3, meter.getMeterNumber());
            ps.executeUpdate();
        }
    }

    /**
     * Insert only if the meter does not already exist. Validates customer ownership.
     */
    public void upsert(Connection connection, int meterId, int customerId) throws SQLException {
        Optional<Meter> existing = findById(connection, meterId);
        if (existing.isPresent()) {
            if (existing.get().getCustomerId() != customerId) {
                throw new IllegalArgumentException(
                        "Meter " + meterId + " belongs to customer "
                                + existing.get().getCustomerId()
                                + " but CSV has customer " + customerId
                );
            }
            return; // already exists and is consistent
        }
        Meter m = new Meter(meterId, customerId, "MTR-" + meterId);
        insert(connection, m);
    }
}
