package com.ecovolt.strategy;

import com.ecovolt.model.UnitPrice;

import java.util.List;

/**
 * Builds the three PricingStrategy instances from the unit_prices table data.
 * <p>
 * This class is pure (no DB calls, no side effects) — easy to unit test:
 * new PricingStrategyFactory(List.of(new UnitPrice(1,"PEAK",9.0), ...))
 */
public class PricingStrategyFactory {

    // Fallback defaults if a row is missing from the DB
    private static final double DEFAULT_OFF_PEAK = 5.0;
    private static final double DEFAULT_STANDARD = 4.0;
    private static final double DEFAULT_PEAK = 7.0;
    private static final double DEFAULT_WEEKEND = 3.0;

    private final PricingStrategy offPeak;
    private final PricingStrategy standard;
    private final PricingStrategy peak;
    private final PricingStrategy weekend;

    public PricingStrategyFactory(List<UnitPrice> unitPrices) {
        double offPeakRate = DEFAULT_OFF_PEAK;
        double standardRate = DEFAULT_STANDARD;
        double peakRate = DEFAULT_PEAK;
        double weekendRate = DEFAULT_WEEKEND;

        for (UnitPrice up : unitPrices) {
            switch (up.getIntervalType()) {
                case "OFF_PEAK" -> offPeakRate = up.getPricePerUnit();
                case "STANDARD" -> standardRate = up.getPricePerUnit();
                case "PEAK" -> peakRate = up.getPricePerUnit();
                case "WEEKEND" -> weekendRate = up.getPricePerUnit();
            }
        }

        this.offPeak = new OffPeakPricingStrategy(offPeakRate);
        this.standard = new StandardPricingStrategy(standardRate);
        this.peak = new PeakPricingStrategy(peakRate);
        this.weekend = new WeekendPricingStrategy(weekendRate);
    }

    public PricingStrategy offPeak() {
        return offPeak;
    }

    public PricingStrategy standard() {
        return standard;
    }

    public PricingStrategy peak() {
        return peak;
    }

    public PricingStrategy weekend() {
        return weekend;
    }
}
