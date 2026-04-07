package com.ecovolt.app;

import com.ecovolt.config.DatabaseConfig;
import com.ecovolt.dao.BillingDAO;
import com.ecovolt.dao.CustomerDAO;
import com.ecovolt.dao.MeterDAO;
import com.ecovolt.dao.UnitPriceDAO;
import com.ecovolt.model.CsvRow;
import com.ecovolt.model.CustomerBill;
import com.ecovolt.model.UnitPrice;
import com.ecovolt.strategy.PricingStrategyFactory;

import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Batch Processing Engine — orchestrator only.
 * All real logic lives in CsvReader, BillingProcessor, and the DAOs.
 * <p>
 * Flow:
 * 1. Load unit prices from DB → build PricingStrategyFactory
 * 2. Read 10,000 rows from CSV
 * 3. Upsert customers + meters referenced in CSV
 * 4. BillingProcessor calculates cost per row using Strategy Pattern
 * 5. JDBC batch-insert one bill per customer
 */
public class MainApplication {

    private static final String DEFAULT_CSV = "meter_readings.csv";

    public static void main(String[] args) throws Exception {
        String csvPath = args.length > 0 ? args[0] : DEFAULT_CSV;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                run(conn, csvPath);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    static void run(Connection conn, String csvPath) throws Exception {

        // Step 1 — Load unit prices from DB and build strategies
        List<UnitPrice> unitPrices = new UnitPriceDAO().findAll(conn);
        PricingStrategyFactory strategies = new PricingStrategyFactory(unitPrices);

        System.out.printf("Rates loaded — OFF_PEAK: %.2f  STANDARD: %.2f  PEAK: %.2f%n",
                strategies.offPeak().calculateCost(1),
                strategies.standard().calculateCost(1),
                strategies.peak().calculateCost(1));

        // Step 2 — Read CSV
        List<CsvRow> rows = new CsvReader().read(csvPath);
        System.out.println("CSV rows read      : " + rows.size());

        // Step 3 — Upsert customers and meters (each customer can have 1 or 2 meters)
        CustomerDAO customerDAO = new CustomerDAO();
        MeterDAO meterDAO = new MeterDAO();
        Set<Integer> seenCustomers = new LinkedHashSet<>();
        Set<Integer> seenMeters = new LinkedHashSet<>();

        for (CsvRow row : rows) {
            if (seenCustomers.add(row.getCustomerId())) {
                customerDAO.upsert(conn, row.getCustomerId());
            }
            if (seenMeters.add(row.getMeterId())) {
                meterDAO.upsert(conn, row.getMeterId(), row.getCustomerId());
            }
        }
        System.out.println("Customers upserted : " + seenCustomers.size());
        System.out.println("Meters upserted    : " + seenMeters.size());

        // Step 4 — Calculate bills using Strategy Pattern
        Collection<CustomerBill> bills = new BillingProcessor(strategies).process(rows);

        // Step 5 — JDBC batch insert
        int inserted = new BillingDAO().batchInsertBills(conn, bills);
        System.out.println("Bills inserted     : " + inserted);

        printSummary(bills);
    }

    private static void printSummary(Collection<CustomerBill> bills) {
        System.out.println("\n--- Bill Summary ---");
        System.out.printf("%-12s  %12s  %12s  %12s  %14s%n",
                "Customer", "OffPeak kWh", "Std kWh", "Peak kWh", "Amount (₹)");
        System.out.println("-".repeat(68));
        double grandUnits = 0, grandAmount = 0;
        for (CustomerBill b : bills) {
            System.out.printf("%-12d  %12.2f  %12.2f  %12.2f  %14.2f%n",
                    b.getCustomerId(),
                    b.getOffPeakUnits(), b.getStandardUnits(), b.getPeakUnits(),
                    b.getTotalAmount());
            grandUnits += b.getTotalUnits();
            grandAmount += b.getTotalAmount();
        }
        System.out.println("-".repeat(68));
        System.out.printf("%-12s  %38.2f  %14.2f%n", "TOTAL", grandUnits, grandAmount);
    }
}
