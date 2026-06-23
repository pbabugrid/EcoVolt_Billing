package com.ecovolt.billing.meter.dto;


import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterStatus;

import java.time.LocalDate;

public record MeterResponse(
        Long id,
        String meterNumber,
        LocalDate installationDate,
        MeterStatus status,
        Long customerId
) {
    public static MeterResponse from(Meter m) {
        return new MeterResponse(
                m.getId(),
                m.getMeterNumber(),
                m.getInstallationDate(),
                m.getStatus(),
                m.getCustomer().getId());
    }
}
