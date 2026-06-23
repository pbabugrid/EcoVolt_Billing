package com.ecovolt.billing.invoice;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerService;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import com.ecovolt.billing.tariff.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Billing service.
 * Pipeline: find latest two readings -> compute consumption -> apply tariff -> persist invoice.
 */
@Service
@RequiredArgsConstructor
public class InvoiceGenerationService {

    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;
    private final TariffService tariffService;

    @Transactional
    public InvoiceResponse generateForCustomer(Long customerId) {
        Customer customer = customerService.getCustomerOrThrow(customerId);

        List<MeterReading> readings =
                meterReadingRepository.findTop2ByMeter_Customer_IdOrderByReadingDateDescIdDesc(customerId);

        if (readings.size() < 2) {
            throw new BillingException(
                    "At least two meter readings are required to generate an invoice for customer id %d"
                            .formatted(customerId));
        }

        MeterReading current = readings.get(0);
        MeterReading previous = readings.get(1);

        if (invoiceRepository.existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
                customerId, previous.getId(), current.getId())) {
            throw new BillingException(
                    "An invoice already exists for customer id %d using readings %d and %d"
                            .formatted(customerId, previous.getId(), current.getId()));
        }

        BigDecimal currentReading = current.getReadingValue();
        BigDecimal previousReading = previous.getReadingValue();
        BigDecimal unitsConsumed = currentReading.subtract(previousReading);

        if (unitsConsumed.signum() < 0) {
            throw new BillingException(
                    "Current reading (%s) is lower than previous reading (%s); cannot bill negative consumption"
                            .formatted(currentReading, previousReading));
        }

        BigDecimal amount = tariffService.calculateAmount(unitsConsumed);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .customer(customer)
                .previousReading(previousReading)
                .currentReading(currentReading)
                .unitsConsumed(unitsConsumed)
                .amount(amount)
                .generatedDate(LocalDate.now())
                .status(InvoiceStatus.GENERATED)
                .previousReadingRecord(previous)
                .currentReadingRecord(current)
                .build();

        return InvoiceResponse.from(invoiceRepository.save(invoice));
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
