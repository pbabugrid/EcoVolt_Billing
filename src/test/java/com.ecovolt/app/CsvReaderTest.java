package com.ecovolt.app;

import com.ecovolt.model.CsvRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {

    // ── helper ────────────────────────────────────────────────────────────────

    private static final String HEADER =
            "customer_id,meter_id,reading_date,off_peak_units,standard_units,peak_units\n";

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void parsesAllSixFields_correctly() throws IOException {
        String content = HEADER + "1,1,2026-03-01,1.50,2.00,1.75\n";

        CsvRow row = new CsvReader().read(csv(content)).get(0);

        assertEquals(1, row.getCustomerId());
        assertEquals(1, row.getMeterId());
        assertEquals(LocalDate.of(2026, 3, 1), row.getReadingDate());
        assertEquals(1.50, row.getOffPeakUnits(), 0.001);
        assertEquals(2.00, row.getStandardUnits(), 0.001);
        assertEquals(1.75, row.getPeakUnits(), 0.001);
    }

    @Test
    void parsesMultipleRows() throws IOException {
        String content = HEADER
                + "1,1,2026-03-01,1.00,2.00,1.50\n"
                + "2,2,2026-03-02,2.00,3.00,2.50\n";

        List<CsvRow> rows = new CsvReader().read(csv(content));

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getCustomerId());
        assertEquals(2, rows.get(1).getCustomerId());
    }

    @Test
    void skipsBlankLines() throws IOException {
        String content = HEADER
                + "1,1,2026-03-01,1.00,2.00,1.50\n"
                + "\n"
                + "2,2,2026-03-02,1.00,2.00,1.50\n";

        assertEquals(2, new CsvReader().read(csv(content)).size());
    }

    @Test
    void headerOnlyFile_returnsEmptyList() throws IOException {
        assertTrue(new CsvReader().read(csv(HEADER)).isEmpty());
    }

    @Test
    void zeroUnitsAllowed() throws IOException {
        String content = HEADER + "1,1,2026-03-01,0.00,0.00,0.00\n";

        CsvRow row = new CsvReader().read(csv(content)).get(0);

        assertEquals(0.0, row.getOffPeakUnits(), 0.001);
        assertEquals(0.0, row.getStandardUnits(), 0.001);
        assertEquals(0.0, row.getPeakUnits(), 0.001);
    }

    @Test
    void trimsWhitespaceAroundValues() throws IOException {
        String content = HEADER + " 3 , 5 ,2026-03-01, 1.00 , 2.00 , 1.50 \n";

        CsvRow row = new CsvReader().read(csv(content)).get(0);

        assertEquals(3, row.getCustomerId());
        assertEquals(5, row.getMeterId());
    }

    // ── read(String path) — file system branch ────────────────────────────────

    @Test
    void readFromFilePath_parsesCorrectly(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, HEADER + "7,7,2026-03-10,1.00,2.00,3.00\n");

        List<CsvRow> rows = new CsvReader().read(csvFile.toString());

        assertEquals(1, rows.size());
        assertEquals(7, rows.get(0).getCustomerId());
    }

    // ── validation errors ─────────────────────────────────────────────────────

    @Test
    void tooFewColumns_throwsIllegalArgumentException() {
        String content = HEADER + "1,1,2026-03-01,1.00\n"; // only 4 columns

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void customerIdZero_throwsIllegalArgumentException() {
        String content = HEADER + "0,1,2026-03-01,1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void customerIdNegative_throwsIllegalArgumentException() {
        String content = HEADER + "-1,1,2026-03-01,1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void customerIdNotANumber_throwsIllegalArgumentException() {
        String content = HEADER + "abc,1,2026-03-01,1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void meterIdZero_throwsIllegalArgumentException() {
        String content = HEADER + "1,0,2026-03-01,1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void meterIdNotANumber_throwsIllegalArgumentException() {
        String content = HEADER + "1,xyz,2026-03-01,1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void negativeOffPeakUnits_throwsIllegalArgumentException() {
        String content = HEADER + "1,1,2026-03-01,-1.00,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void negativeStandardUnits_throwsIllegalArgumentException() {
        String content = HEADER + "1,1,2026-03-01,1.00,-2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void negativePeakUnits_throwsIllegalArgumentException() {
        String content = HEADER + "1,1,2026-03-01,1.00,2.00,-1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void nonNumericOffPeakUnits_throwsIllegalArgumentException() {
        String content = HEADER + "1,1,2026-03-01,bad,2.00,1.50\n";

        assertThrows(IllegalArgumentException.class,
                () -> new CsvReader().read(csv(content)));
    }

    @Test
    void invalidDate_throwsException() {
        String content = HEADER + "1,1,not-a-date,1.00,2.00,1.50\n";

        assertThrows(Exception.class,
                () -> new CsvReader().read(csv(content)));
    }
}
