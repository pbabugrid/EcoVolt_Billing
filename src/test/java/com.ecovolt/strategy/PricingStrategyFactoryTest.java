package com.ecovolt.strategy;

import com.ecovolt.model.UnitPrice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests PricingStrategyFactory — builds the three strategies from DB-loaded UnitPrice list.
 * Pure — no DB calls needed.
 */
class PricingStrategyFactoryTest {

    @Test
    void allThreeRates_loadedFromUnitPriceList() {
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "OFF_PEAK", 3.0),
                new UnitPrice(2, "STANDARD", 5.0),
                new UnitPrice(3, "PEAK", 9.0)
        ));

        assertEquals(6.0, factory.offPeak().calculateCost(2.0), 0.001);
        assertEquals(15.0, factory.standard().calculateCost(3.0), 0.001);
        assertEquals(18.0, factory.peak().calculateCost(2.0), 0.001);
    }

    @Test
    void emptyList_allStrategiesFallBackToDefaults() {
        var factory = new PricingStrategyFactory(List.of());

        // defaults: OFF_PEAK=5.0, STANDARD=4.0, PEAK=7.0
        assertEquals(5.0, factory.offPeak().calculateCost(1.0), 0.001);
        assertEquals(4.0, factory.standard().calculateCost(1.0), 0.001);
        assertEquals(7.0, factory.peak().calculateCost(1.0), 0.001);
    }

    @Test
    void onlyPeakProvided_othersTwoUseDefaults() {
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "PEAK", 10.0)
        ));

        assertEquals(5.0, factory.offPeak().calculateCost(1.0), 0.001); // default
        assertEquals(4.0, factory.standard().calculateCost(1.0), 0.001); // default
        assertEquals(10.0, factory.peak().calculateCost(1.0), 0.001); // overridden
    }

    @Test
    void onlyOffPeakProvided_othersTwoUseDefaults() {
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "OFF_PEAK", 2.0)
        ));

        assertEquals(2.0, factory.offPeak().calculateCost(1.0), 0.001); // overridden
        assertEquals(4.0, factory.standard().calculateCost(1.0), 0.001); // default
        assertEquals(7.0, factory.peak().calculateCost(1.0), 0.001); // default
    }

    @Test
    void onlyStandardProvided_othersTwoUseDefaults() {
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "STANDARD", 6.0)
        ));

        assertEquals(5.0, factory.offPeak().calculateCost(1.0), 0.001); // default
        assertEquals(6.0, factory.standard().calculateCost(1.0), 0.001); // overridden
        assertEquals(7.0, factory.peak().calculateCost(1.0), 0.001); // default
    }

    @Test
    void unknownIntervalType_isIgnored_defaultsUsed() {
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "UNKNOWN_TYPE", 99.0)
        ));

        // all three should be defaults since the unknown type is ignored
        assertEquals(5.0, factory.offPeak().calculateCost(1.0), 0.001);
        assertEquals(4.0, factory.standard().calculateCost(1.0), 0.001);
        assertEquals(7.0, factory.peak().calculateCost(1.0), 0.001);
    }

    @Test
    void strategyNames_matchExpectedIntervalTypes() {
        var factory = new PricingStrategyFactory(List.of());

        assertEquals("OFF_PEAK", factory.offPeak().getName());
        assertEquals("STANDARD", factory.standard().getName());
        assertEquals("PEAK", factory.peak().getName());
    }

    @Test
    void laterDuplicateEntry_overridesEarlierOneForSameInterval() {
        // Two PEAK entries — the last one should win (loop overwrites)
        var factory = new PricingStrategyFactory(List.of(
                new UnitPrice(1, "PEAK", 7.0),
                new UnitPrice(2, "PEAK", 12.0)
        ));

        assertEquals(12.0, factory.peak().calculateCost(1.0), 0.001);
    }
}
