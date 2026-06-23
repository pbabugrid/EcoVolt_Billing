package com.ecovolt.billing.tariff.dto;

import com.ecovolt.billing.tariff.TariffSlab;

import java.math.BigDecimal;

public record TariffSlabResponse(
        Long id,
        Integer sortOrder,
        BigDecimal fromUnits,
        BigDecimal toUnits,
        BigDecimal ratePerUnit
) {
    public static TariffSlabResponse from(TariffSlab slab) {
        return new TariffSlabResponse(
                slab.getId(),
                slab.getSortOrder(),
                slab.getFromUnits(),
                slab.getToUnits(),
                slab.getRatePerUnit());
    }
}
