package com.ecovolt.billing.reading.dto;



import com.ecovolt.billing.reading.MeterReading;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeterReadingResponse(
        Long id,
        Long meterId,
        LocalDate readingDate,
        BigDecimal readingValue
) {
    public static MeterReadingResponse from(MeterReading r) {
        return new MeterReadingResponse(
                r.getId(),
                r.getMeter().getId(),
                r.getReadingDate(),
                r.getReadingValue());
    }
}
