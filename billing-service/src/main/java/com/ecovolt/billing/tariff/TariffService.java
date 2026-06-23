package com.ecovolt.billing.tariff;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Flat-rate tariff: 1 unit = INR 5.
 * Isolated so a slab/time-of-use tariff can later replace it without touching billing logic.
 */
@Service
public class TariffService {

    public static final BigDecimal RATE_PER_UNIT = new BigDecimal("5");

    public BigDecimal calculateAmount(BigDecimal unitsConsumed) {
        return unitsConsumed.multiply(RATE_PER_UNIT).setScale(2, RoundingMode.HALF_UP);
    }
}
