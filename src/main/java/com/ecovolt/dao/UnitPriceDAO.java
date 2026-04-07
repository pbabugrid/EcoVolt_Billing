package com.ecovolt.dao;

import com.ecovolt.model.UnitPrice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads pricing rates from the unit_prices table.
 * Takes Connection as a parameter — no static state, easy to test.
 */
public class UnitPriceDAO {

    private static final String SELECT_ALL =
            "SELECT price_id, interval_type, price_per_unit FROM unit_prices";

    public List<UnitPrice> findAll(Connection connection) throws SQLException {
        List<UnitPrice> prices = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UnitPrice up = new UnitPrice();
                up.setPriceId(rs.getInt("price_id"));
                up.setIntervalType(rs.getString("interval_type"));
                up.setPricePerUnit(rs.getDouble("price_per_unit"));
                prices.add(up);
            }
        }

        return prices;
    }
}
