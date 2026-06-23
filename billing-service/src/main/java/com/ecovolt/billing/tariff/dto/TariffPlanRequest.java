package com.ecovolt.billing.tariff.dto;

import com.ecovolt.billing.tariff.TariffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TariffPlanRequest(

        @NotNull(message = "type is required")
        TariffType type,

        @NotNull(message = "version is required")
        @Positive(message = "version must be positive")
        Integer version,

        @NotBlank(message = "name is required")
        @Size(max = 120, message = "name must not exceed 120 characters")
        String name,

        @NotNull(message = "effectiveFrom is required")
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        @Valid
        @NotEmpty(message = "at least one slab is required")
        List<TariffSlabRequest> slabs
) {
}
