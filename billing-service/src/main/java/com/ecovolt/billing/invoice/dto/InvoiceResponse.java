package com.ecovolt.billing.invoice.dto;

import com.ecovolt.billing.invoice.Invoice;
import com.ecovolt.billing.invoice.InvoiceStatus;
import com.ecovolt.billing.tariff.TariffType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long customerId,
        Long previousReadingId,
        Long currentReadingId,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal unitsConsumed,
        BigDecimal amount,
        Long tariffPlanId,
        TariffType tariffType,
        Integer tariffVersion,
        LocalDate generatedDate,
        InvoiceStatus status
) {
    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(
                i.getId(),
                i.getInvoiceNumber(),
                i.getCustomer().getId(),
                i.getPreviousReadingRecord().getId(),
                i.getCurrentReadingRecord().getId(),
                i.getPreviousReading(),
                i.getCurrentReading(),
                i.getUnitsConsumed(),
                i.getAmount(),
                i.getTariffPlan() == null ? null : i.getTariffPlan().getId(),
                i.getTariffType(),
                i.getTariffVersion(),
                i.getGeneratedDate(),
                i.getStatus());
    }
}
