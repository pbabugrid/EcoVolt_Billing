package com.ecovolt.strategy;

public class OffPeakPricingStrategy implements PricingStrategy {
    private final double ratePerUnit;

    public OffPeakPricingStrategy(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    @Override
    public String getName() {
        return "OFF_PEAK";
    }

    @Override
    public double calculateCost(double units) {
        return units * ratePerUnit;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
    }
}
