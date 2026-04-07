package com.ecovolt.strategy;

/**
 * Strategy Pattern — each implementation knows its own rate
 * and can calculate the cost for a given number of units.
 */
public interface PricingStrategy {
    String getName();

    double calculateCost(double units);
}
