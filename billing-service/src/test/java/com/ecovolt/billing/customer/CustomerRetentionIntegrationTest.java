package com.ecovolt.billing.customer;

import com.ecovolt.billing.invoice.Invoice;
import com.ecovolt.billing.invoice.InvoiceRepository;
import com.ecovolt.billing.invoice.InvoiceStatus;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.meter.MeterStatus;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerRetentionIntegrationTest {

    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Deleting a customer with billing history deactivates and preserves invoices")
    void deleteCustomerWithBillingHistory_deactivatesAndPreservesInvoices() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-RETENTION")
                .name("Retention Tester")
                .email("retention@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build());
        Meter meter = meterRepository.save(Meter.builder()
                .meterNumber("MTR-RETENTION")
                .installationDate(LocalDate.of(2023, 1, 1))
                .status(MeterStatus.ACTIVE)
                .customer(customer)
                .build());
        MeterReading previous = persistReading(meter, "2024-01-01", "100.00");
        MeterReading current = persistReading(meter, "2024-02-01", "140.00");
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-RETENTION")
                .customer(customer)
                .previousReading(new BigDecimal("100.00"))
                .currentReading(new BigDecimal("140.00"))
                .unitsConsumed(new BigDecimal("40.00"))
                .amount(new BigDecimal("200.00"))
                .generatedDate(LocalDate.of(2024, 2, 1))
                .status(InvoiceStatus.GENERATED)
                .previousReadingRecord(previous)
                .currentReadingRecord(current)
                .build());

        mockMvc.perform(delete("/api/customers/{id}", customer.getId()))
                .andExpect(status().isNoContent());

        assertThat(customerRepository.findById(customer.getId()))
                .get()
                .extracting(Customer::getStatus)
                .isEqualTo(CustomerStatus.INACTIVE);
        assertThat(invoiceRepository.findById(invoice.getId())).isPresent();
    }

    private MeterReading persistReading(Meter meter, String date, String value) {
        return meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.parse(date))
                .readingValue(new BigDecimal(value))
                .build());
    }
}
