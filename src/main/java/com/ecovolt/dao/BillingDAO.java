package com.ecovolt.dao;

import com.ecovolt.model.CustomerBill;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Persists customer bills using JDBC batch processing.
 * bills.customer_id is a FK to customers.customer_id.
 * Takes Connection as a parameter — no static state, easy to test.
 */
public class BillingDAO {

    private static final String INSERT_BILL =
            "INSERT INTO bills " +
                    "(customer_id, billing_start, billing_end, " +
                    " off_peak_units, standard_units, peak_units, total_units, total_amount) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final int BATCH_SIZE = 5;

    public int batchInsertBills(Connection connection, Collection<CustomerBill> bills)
            throws SQLException {

        int totalInserted = 0;
        int count = 0;

        try (PreparedStatement ps = connection.prepareStatement(INSERT_BILL)) {
            for (CustomerBill bill : bills) {
                ps.setInt(1, bill.getCustomerId());
                ps.setDate(2, Date.valueOf(bill.getBillingStart()));
                ps.setDate(3, Date.valueOf(bill.getBillingEnd()));
                ps.setDouble(4, bill.getOffPeakUnits());
                ps.setDouble(5, bill.getStandardUnits());
                ps.setDouble(6, bill.getPeakUnits());
                ps.setDouble(7, bill.getTotalUnits());
                ps.setDouble(8, bill.getTotalAmount());
                ps.addBatch();
                count++;

                if (count % BATCH_SIZE == 0) {
                    totalInserted += sum(ps.executeBatch());
                }
            }
            if (count % BATCH_SIZE != 0) {
                totalInserted += sum(ps.executeBatch());
            }
        }

        return totalInserted;
    }

    private static int sum(int[] results) {
        int n = 0;
        for (int r : results) {
            if (r > 0 || r == PreparedStatement.SUCCESS_NO_INFO) n++;
        }
        return n;
    }
}
