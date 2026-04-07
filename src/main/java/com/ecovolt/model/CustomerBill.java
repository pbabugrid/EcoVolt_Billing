package com.ecovolt.model;

import java.time.LocalDate;

/**
 * Accumulated bill for one customer across all their CSV rows.
 * Linked to customers table via customer_id (FK).
 */
public class CustomerBill {
    private final int customerId;
    private LocalDate billingStart;
    private LocalDate billingEnd;
    private double offPeakUnits;
    private double standardUnits;
    private double peakUnits;
    private double totalUnits;
    private double totalAmount;

    public CustomerBill(int customerId) {
        this.customerId = customerId;
    }

    public void add(LocalDate date,
                    double offPeak, double standard, double peak,
                    double cost) {
        this.offPeakUnits += offPeak;
        this.standardUnits += standard;
        this.peakUnits += peak;
        this.totalUnits += offPeak + standard + peak;
        this.totalAmount += cost;

        if (billingStart == null || date.isBefore(billingStart)) billingStart = date;
        if (billingEnd == null || date.isAfter(billingEnd)) billingEnd = date;
    }

    public int getCustomerId() {
        return customerId;
    }

    public LocalDate getBillingStart() {
        return billingStart;
    }

    public LocalDate getBillingEnd() {
        return billingEnd;
    }

    public double getOffPeakUnits() {
        return offPeakUnits;
    }

    public double getStandardUnits() {
        return standardUnits;
    }

    public double getPeakUnits() {
        return peakUnits;
    }

    public double getTotalUnits() {
        return totalUnits;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
