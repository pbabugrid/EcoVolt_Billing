package com.ecovolt.model;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One parsed row from the CSV file.
 * customer_id, meter_id, reading_date, off_peak_units, standard_units, peak_units
 */
public class CsvRow {
    private final int customerId;
    private final int meterId;
    private final LocalDate readingDate;
    private final double offPeakUnits;
    private final double standardUnits;
    private final double peakUnits;

    @JsonCreator
    public CsvRow(@JsonProperty("customerId") int customerId,
                  @JsonProperty("meterId") int meterId,
                  @JsonProperty("readingDate") LocalDate readingDate,
                  @JsonProperty("offPeakUnits") double offPeakUnits,
                  @JsonProperty("standardUnits") double standardUnits,
                  @JsonProperty("peakUnits") double peakUnits) {
        this.customerId = customerId;
        this.meterId = meterId;
        this.readingDate = readingDate;
        this.offPeakUnits = offPeakUnits;
        this.standardUnits = standardUnits;
        this.peakUnits = peakUnits;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getMeterId() {
        return meterId;
    }

    public LocalDate getReadingDate() {
        return readingDate;
    }

    public double getOffPeakUnits() {
        return offPeakUnits;
    }

    public double getStandardUnits() {
        return standardUnits;
    }

    public double getPeakUnits() {
        return peakUnits;
    }
}
