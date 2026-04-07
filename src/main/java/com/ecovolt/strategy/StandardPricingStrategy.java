package com.ecovolt.strategy;

public class StandardPricingStrategy implements PricingStrategy {
    private final double ratePerUnit;

    public StandardPricingStrategy(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    @Override
    public String getName() {
        return "STANDARD";
    }

    @Override
    public double calculateCost(double units) {
        return units * ratePerUnit;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
    }
}
