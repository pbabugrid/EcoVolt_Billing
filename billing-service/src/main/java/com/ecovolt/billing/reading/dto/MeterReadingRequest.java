package com.ecovolt.billing.reading.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeterReadingRequest(

        @NotNull(message = "meterId is required")
        Long meterId,

        @NotNull(message = "readingDate is required")
        @PastOrPresent(message = "readingDate cannot be in the future")
        LocalDate readingDate,

        @NotNull(message = "readingValue is required")
        @PositiveOrZero(message = "readingValue must be zero or positive")
        BigDecimal readingValue
) {
}
