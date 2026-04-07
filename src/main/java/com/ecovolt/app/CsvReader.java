package com.ecovolt.app;

import com.ecovolt.model.CsvRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the meter_readings CSV into a list of CsvRow objects.
 * No DB calls, no cost calculations — single responsibility, easy to unit test.
 * <p>
 * CSV format: customer_id, meter_id, reading_date, off_peak_units, standard_units, peak_units
 */
public class CsvReader {

    public List<CsvRow> read(String csvPath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(csvPath);
        if (inputStream == null) {
            inputStream = Files.newInputStream(Path.of(csvPath));
        }
        return read(inputStream);
    }

    /**
     * Accepts an InputStream directly — makes it trivial to test with in-memory CSV strings.
     */
    public List<CsvRow> read(InputStream inputStream) throws IOException {
        List<CsvRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            reader.readLine(); // skip header line

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                String[] col = line.split(",", -1);
                if (col.length < 6) {
                    throw new IllegalArgumentException(
                            "Malformed CSV at line " + lineNum +
                                    " — expected 6 columns, got " + col.length);
                }

                int customerId = parsePositiveInt(col[0].trim(), lineNum, "customer_id");
                int meterId = parsePositiveInt(col[1].trim(), lineNum, "meter_id");
                LocalDate readingDate = LocalDate.parse(col[2].trim());
                double offPeakUnits = parseNonNegativeDouble(col[3].trim(), lineNum, "off_peak_units");
                double standardUnits = parseNonNegativeDouble(col[4].trim(), lineNum, "standard_units");
                double peakUnits = parseNonNegativeDouble(col[5].trim(), lineNum, "peak_units");

                rows.add(new CsvRow(customerId, meterId, readingDate,
                        offPeakUnits, standardUnits, peakUnits));
            }
        }

        return rows;
    }

    private static int parsePositiveInt(String val, int line, String col) {
        try {
            int n = Integer.parseInt(val);
            if (n <= 0) throw new IllegalArgumentException(col + " must be > 0 at line " + line);
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad integer for " + col + " at line " + line, e);
        }
    }

    private static double parseNonNegativeDouble(String val, int line, String col) {
        try {
            double d = Double.parseDouble(val);
            if (d < 0) throw new IllegalArgumentException(col + " cannot be negative at line " + line);
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number for " + col + " at line " + line, e);
        }
    }
}
