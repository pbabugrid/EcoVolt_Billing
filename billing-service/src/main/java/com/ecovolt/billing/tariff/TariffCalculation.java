package com.ecovolt.billing.tariff;

import java.math.BigDecimal;

public record TariffCalculation(
        BigDecimal amount,
        TariffPlan tariffPlan
) {
}
