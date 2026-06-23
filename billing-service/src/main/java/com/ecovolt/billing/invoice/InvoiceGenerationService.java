package com.ecovolt.billing.invoice;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerService;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import com.ecovolt.billing.tariff.TariffCalculation;
import com.ecovolt.billing.tariff.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Billing service.
 * Pipeline: for each customer meter with at least two readings, use the latest two readings
 * from that same meter to compute consumption, apply tariff, and persist an invoice.
 * Duplicate source-reading pairs are skipped; if all eligible pairs are already invoiced, throws 422.
 */
@Service
@RequiredArgsConstructor
public class InvoiceGenerationService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;
    private final TariffService tariffService;

    @Transactional
    public List<InvoiceResponse> generateForCustomer(Long customerId) {
        Customer customer = customerService.getCustomerOrThrow(customerId);

        List<Meter> meters = meterRepository.findByCustomer_IdOrderByIdAsc(customerId);

        boolean anyMeterHasTwoReadings = false;
        List<InvoiceResponse> created = new ArrayList<>();

        for (Meter meter : meters) {
            List<MeterReading> readings =
                    meterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc(meter.getId());

            if (readings.size() < 2) {
                continue;
            }
            anyMeterHasTwoReadings = true;

            MeterReading current  = readings.get(0);
            MeterReading previous = readings.get(1);

            // Skip duplicate pairs silently within a batch.
            if (invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                    customerId, previous.getId(), current.getId())) {
                continue;
            }

            BigDecimal currentValue  = current.getReadingValue();
            BigDecimal previousValue = previous.getReadingValue();
            BigDecimal unitsConsumed = currentValue.subtract(previousValue);

            if (unitsConsumed.signum() < 0) {
                throw new BillingException(
                        "Current reading (%s) is lower than previous reading (%s) for meter %s; cannot bill negative consumption"
                                .formatted(currentValue, previousValue, meter.getMeterNumber()));
            }

            LocalDate generatedDate = LocalDate.now();
            TariffCalculation calculation = tariffService.calculateAmount(
                    meter.getTariffType(), unitsConsumed, generatedDate);

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(generateInvoiceNumber())
                    .customer(customer)
                    .previousReading(previousValue)
                    .currentReading(currentValue)
                    .unitsConsumed(unitsConsumed)
                    .amount(calculation.amount())
                    .tariffPlan(calculation.tariffPlan())
                    .tariffType(calculation.tariffPlan().getType())
                    .tariffVersion(calculation.tariffPlan().getVersion())
                    .generatedDate(generatedDate)
                    .status(InvoiceStatus.GENERATED)
                    .previousReadingRecord(previous)
                    .currentReadingRecord(current)
                    .build();

            created.add(InvoiceResponse.from(invoiceRepository.save(invoice)));
        }

        if (!anyMeterHasTwoReadings) {
            throw new BillingException(
                    "At least two meter readings are required to generate an invoice for customer id %d"
                            .formatted(customerId));
        }

        if (created.isEmpty()) {
            throw new BillingException(
                    "All eligible meter reading pairs for customer id %d have already been invoiced"
                            .formatted(customerId));
        }

        return created;
    }

    private String generateInvoiceNumber() {
        String candidate;
        do {
            candidate = "INV-" + LocalDate.now().getYear() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (invoiceRepository.existsByInvoiceNumber(candidate));
        return candidate;
    }
}
