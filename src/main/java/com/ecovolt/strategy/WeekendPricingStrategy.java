package com.ecovolt.strategy;

public class WeekendPricingStrategy implements PricingStrategy {
    private final double ratePerUnit;

    public WeekendPricingStrategy(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    @Override
    public String getName() {
        return "WEEKEND";
    }

    @Override
    public double calculateCost(double units) {
        return units * ratePerUnit;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
    }
}

