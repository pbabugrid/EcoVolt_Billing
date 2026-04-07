package com.ecovolt.model;

public class Meter {
    private int meterId;
    private int customerId;
    private String meterNumber;

    public Meter() {
    }

    public Meter(int meterId, int customerId, String meterNumber) {
        this.meterId = meterId;
        this.customerId = customerId;
        this.meterNumber = meterNumber;
    }

    public int getMeterId() {
        return meterId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getMeterNumber() {
        return meterNumber;
    }

    public void setMeterId(int meterId) {
        this.meterId = meterId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setMeterNumber(String n) {
        this.meterNumber = n;
    }
}
