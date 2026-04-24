package com.ecovolt.app;

import com.ecovolt.model.CustomerBill;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonExporterTest {

    @TempDir
    Path tempDir;

    public JsonExporterTest() {
    }

    @Test
    void testExportBillsToJson_CreatesFileAndSerializesCorrectly() throws IOException {
        // Arrange
        JsonExporter exporter = new JsonExporter();

        CustomerBill dummyBill = new CustomerBill(1234);
        dummyBill.add(LocalDate.now(), 0.0, 100.0, 0.0, 400.0);

        List<CustomerBill> bills = List.of(dummyBill);

        File outputFile = tempDir.resolve("bills_output.json").toFile();

        // Act
        exporter.exportBillsToJson(bills, outputFile.getAbsolutePath());

        // Assert
        assertTrue(outputFile.exists(), "The JSON file should be created.");
        assertTrue(outputFile.length() > 0, "The JSON file should not be empty.");

        // Optional verification: we could also decode it back using a generic ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // need JavaTimeModule
        CustomerBill[] readBills = mapper.readValue(outputFile, CustomerBill[].class);

        assertTrue(readBills.length == 1);
        assertTrue(readBills[0].getCustomerId() == 1234);
    }
}
