package com.ecovolt.strategy;

public class PeakPricingStrategy implements PricingStrategy {
    private final double ratePerUnit;

    public PeakPricingStrategy(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    @Override
    public String getName() {
        return "PEAK";
    }

    @Override
    public double calculateCost(double units) {
        return units * ratePerUnit;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
    }
}
