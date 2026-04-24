package com.ecovolt.app;

import com.ecovolt.model.CustomerBill;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * Exports processed customer bills to a CSV file.
 * Pure Java implementation for writing batch report results to disk.
 */
public class CsvExporter {

    /**
     * Writes a collection of customer bills to the specified file path.
     *
     * @param bills    The processed bills to export
     * @param filePath The destination path for the CSV file
     * @throws IOException If disk I/O fails
     */
    public void export(Collection<CustomerBill> bills, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write standard CSV header
            writer.write("customer_id,billing_start,billing_end,off_peak_units,standard_units,peak_units,total_units,total_amount");
            writer.newLine();

            // Write each bill as a CSV row
            for (CustomerBill bill : bills) {
                writer.write(String.format("%d,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                        bill.getCustomerId(),
                        bill.getBillingStart(),
                        bill.getBillingEnd(),
                        bill.getOffPeakUnits(),
                        bill.getStandardUnits(),
                        bill.getPeakUnits(),
                        bill.getTotalUnits(),
                        bill.getTotalAmount()
                ));
                writer.newLine();
            }
        }
    }
}

