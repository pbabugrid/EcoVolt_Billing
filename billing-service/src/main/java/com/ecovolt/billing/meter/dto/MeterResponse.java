package com.ecovolt.billing.meter.dto;


import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterStatus;
import com.ecovolt.billing.tariff.TariffType;

import java.time.LocalDate;

public record MeterResponse(
        Long id,
        String meterNumber,
        LocalDate installationDate,
        MeterStatus status,
        TariffType tariffType,
        Long customerId
) {
    public static MeterResponse from(Meter m) {
        return new MeterResponse(
                m.getId(),
                m.getMeterNumber(),
                m.getInstallationDate(),
                m.getStatus(),
                m.getTariffType(),
                m.getCustomer().getId());
    }
}
