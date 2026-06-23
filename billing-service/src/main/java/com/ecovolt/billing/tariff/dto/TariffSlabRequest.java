package com.ecovolt.billing.tariff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TariffSlabRequest(

        @NotNull(message = "fromUnits is required")
        @DecimalMin(value = "0.00", message = "fromUnits must be zero or positive")
        BigDecimal fromUnits,

        @DecimalMin(value = "0.01", message = "toUnits must be positive")
        BigDecimal toUnits,

        @NotNull(message = "ratePerUnit is required")
        @DecimalMin(value = "0.01", message = "ratePerUnit must be positive")
        BigDecimal ratePerUnit
) {
}
