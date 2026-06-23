package com.ecovolt.billing.meter.dto;

import com.ecovolt.billing.tariff.TariffType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MeterRequest(

        @NotNull(message = "customerId is required")
        Long customerId,

        @NotBlank(message = "meterNumber is required")
        @Size(max = 60, message = "meterNumber must not exceed 60 characters")
        String meterNumber,

        @NotNull(message = "installationDate is required")
        @PastOrPresent(message = "installationDate cannot be in the future")
        LocalDate installationDate,

        TariffType tariffType
) {
}
