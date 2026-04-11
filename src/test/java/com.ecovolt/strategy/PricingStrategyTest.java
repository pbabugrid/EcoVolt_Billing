package com.ecovolt.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests each concrete strategy in isolation.
 * Pure — no DB, no file I/O.
 */
class PricingStrategyTest {

    // ── OffPeakPricingStrategy ────────────────────────────────────────────────

    @Test
    void offPeak_calculateCost_returnsUnitsTimesRate() {
        assertEquals(10.0, new OffPeakPricingStrategy(5.0).calculateCost(2.0), 0.001);
    }

    @Test
    void offPeak_calculateCost_zeroUnits_returnsZero() {
        assertEquals(0.0, new OffPeakPricingStrategy(5.0).calculateCost(0.0), 0.001);
    }

    @Test
    void offPeak_getName_returnsOffPeak() {
        assertEquals("OFF_PEAK", new OffPeakPricingStrategy(5.0).getName());
    }

    @Test
    void offPeak_getRatePerUnit_returnsConstructedRate() {
        assertEquals(5.0, new OffPeakPricingStrategy(5.0).getRatePerUnit(), 0.001);
    }

    // ── StandardPricingStrategy ───────────────────────────────────────────────

    @Test
    void standard_calculateCost_returnsUnitsTimesRate() {
        assertEquals(12.0, new StandardPricingStrategy(4.0).calculateCost(3.0), 0.001);
    }

    @Test
    void standard_calculateCost_zeroUnits_returnsZero() {
        assertEquals(0.0, new StandardPricingStrategy(4.0).calculateCost(0.0), 0.001);
    }

    @Test
    void standard_getName_returnsStandard() {
        assertEquals("STANDARD", new StandardPricingStrategy(4.0).getName());
    }

    @Test
    void standard_getRatePerUnit_returnsConstructedRate() {
        assertEquals(4.0, new StandardPricingStrategy(4.0).getRatePerUnit(), 0.001);
    }

    // ── PeakPricingStrategy ───────────────────────────────────────────────────

    @Test
    void peak_calculateCost_returnsUnitsTimesRate() {
        assertEquals(14.0, new PeakPricingStrategy(7.0).calculateCost(2.0), 0.001);
    }

    @Test
    void peak_calculateCost_zeroUnits_returnsZero() {
        assertEquals(0.0, new PeakPricingStrategy(7.0).calculateCost(0.0), 0.001);
    }

    @Test
    void peak_getName_returnsPeak() {
        assertEquals("PEAK", new PeakPricingStrategy(7.0).getName());
    }

    @Test
    void peak_getRatePerUnit_returnsConstructedRate() {
        assertEquals(7.0, new PeakPricingStrategy(7.0).getRatePerUnit(), 0.001);
    }

    // ── Fractional units ──────────────────────────────────────────────────────

    @Test
    void calculateCost_fractionalUnits_computedCorrectly() {
        assertEquals(7.5, new OffPeakPricingStrategy(5.0).calculateCost(1.5), 0.001);
        assertEquals(6.0, new StandardPricingStrategy(4.0).calculateCost(1.5), 0.001);
        assertEquals(10.5, new PeakPricingStrategy(7.0).calculateCost(1.5), 0.001);
    }
}
