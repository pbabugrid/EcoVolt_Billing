package com.ecovolt.model;

/**
 * One row from the unit_prices table.
 * interval_type is one of: OFF_PEAK, STANDARD, PEAK
 */
public class UnitPrice {
    private int priceId;
    private String intervalType;   // "OFF_PEAK" | "STANDARD" | "PEAK"
    private double pricePerUnit;

    public UnitPrice() {
    }

    public UnitPrice(int priceId, String intervalType, double pricePerUnit) {
        this.priceId = priceId;
        this.intervalType = intervalType;
        this.pricePerUnit = pricePerUnit;
    }

    public int getPriceId() {
        return priceId;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPriceId(int priceId) {
        this.priceId = priceId;
    }

    public void setIntervalType(String t) {
        this.intervalType = t;
    }

    public void setPricePerUnit(double p) {
        this.pricePerUnit = p;
    }
}
