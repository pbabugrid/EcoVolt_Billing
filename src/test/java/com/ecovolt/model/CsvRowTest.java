package com.ecovolt.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CsvRowTest {

    @Test
    void testCsvRowJsonSerializationAndDeserialization() throws Exception {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // We use CsvRow as the closest existing model to "MeterReading"
        CsvRow originalRow = new CsvRow(1001, 555, LocalDate.of(2026, 4, 12), 15.5, 20.0, 5.0);

        // Act - Serialize
        String jsonOutput = mapper.writeValueAsString(originalRow);

        // Assert - Serialize
        assertNotNull(jsonOutput);

        // Act - Deserialize (This will fail unless CsvRow has @JsonCreator or default constructor!)
        CsvRow deserializedRow = mapper.readValue(jsonOutput, CsvRow.class);

        // Assert - Deserialize
        assertEquals(originalRow.getCustomerId(), deserializedRow.getCustomerId());
        assertEquals(originalRow.getMeterId(), deserializedRow.getMeterId());
        assertEquals(originalRow.getReadingDate(), deserializedRow.getReadingDate());
    }
}

