package com.ecovolt.billing.tariff.dto;

import com.ecovolt.billing.tariff.TariffPlan;
import com.ecovolt.billing.tariff.TariffType;

import java.time.LocalDate;
import java.util.List;

public record TariffPlanResponse(
        Long id,
        TariffType type,
        Integer version,
        String name,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        List<TariffSlabResponse> slabs
) {
    public static TariffPlanResponse from(TariffPlan plan) {
        return new TariffPlanResponse(
                plan.getId(),
                plan.getType(),
                plan.getVersion(),
                plan.getName(),
                plan.getEffectiveFrom(),
                plan.getEffectiveTo(),
                plan.isActive(),
                plan.getSlabs().stream()
                        .map(TariffSlabResponse::from)
                        .toList());
    }
}
