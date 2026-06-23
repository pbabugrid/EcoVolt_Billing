package com.ecovolt.billing.invoice;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerRepository;
import com.ecovolt.billing.customer.CustomerStatus;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.meter.MeterStatus;
import com.ecovolt.billing.reading.MeterReading;
import com.ecovolt.billing.reading.MeterReadingRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceLifecycleIntegrationTest {

    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired InvoiceService invoiceService;
    @Autowired MockMvc mockMvc;

    private Customer customer;
    private MeterReading previous;
    private MeterReading current;

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(Customer.builder()
                .customerNumber("CUST-LIFECYCLE")
                .name("Lifecycle Tester")
                .email("lifecycle@ecovolt.test")
                .status(CustomerStatus.ACTIVE)
                .build());
        Meter meter = meterRepository.save(Meter.builder()
                .meterNumber("MTR-LIFECYCLE")
                .installationDate(LocalDate.of(2023, 1, 1))
                .status(MeterStatus.ACTIVE)
                .customer(customer)
                .build());
        previous = persistReading(meter, "2024-01-01", "100.00");
        current = persistReading(meter, "2024-02-01", "150.00");
    }

    @Test
    @DisplayName("Generated invoice can be paid")
    void generatedInvoice_canBePaid() {
        Invoice invoice = persistInvoice("INV-LIFE-PAID", InvoiceStatus.GENERATED);

        assertThat(invoiceService.pay(invoice.getId()).status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoiceRepository.findById(invoice.getId())).get()
                .extracting(Invoice::getStatus)
                .isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Overdue invoice can be cancelled")
    void overdueInvoice_canBeCancelled() {
        Invoice invoice = persistInvoice("INV-LIFE-CANCEL", InvoiceStatus.OVERDUE);

        assertThat(invoiceService.cancel(invoice.getId()).status()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("Paid invoice cannot be cancelled")
    void paidInvoice_cannotBeCancelled() {
        Invoice invoice = persistInvoice("INV-LIFE-INVALID", InvoiceStatus.PAID);

        assertThatThrownBy(() -> invoiceService.cancel(invoice.getId()))
                .hasMessageContaining("cannot transition from PAID to CANCELLED");
    }

    @Test
    @DisplayName("Pay endpoint returns updated invoice")
    void payEndpoint_returnsUpdatedInvoice() throws Exception {
        Invoice invoice = persistInvoice("INV-LIFE-API-PAY", InvoiceStatus.GENERATED);

        mockMvc.perform(post("/api/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.getId()))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Cancel endpoint rejects terminal invoice")
    void cancelEndpoint_rejectsTerminalInvoice() throws Exception {
        Invoice invoice = persistInvoice("INV-LIFE-API-INVALID", InvoiceStatus.CANCELLED);

        mockMvc.perform(post("/api/invoices/{id}/cancel", invoice.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Invoice INV-LIFE-API-INVALID cannot transition from CANCELLED to CANCELLED"));
    }

    private MeterReading persistReading(Meter meter, String date, String value) {
        return meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.parse(date))
                .readingValue(new BigDecimal(value))
                .build());
    }

    private Invoice persistInvoice(String invoiceNumber, InvoiceStatus status) {
        return invoiceRepository.save(Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .previousReading(previous.getReadingValue())
                .currentReading(current.getReadingValue())
                .unitsConsumed(current.getReadingValue().subtract(previous.getReadingValue()))
                .amount(new BigDecimal("250.00"))
                .generatedDate(LocalDate.of(2024, 2, 1))
                .status(status)
                .previousReadingRecord(previous)
                .currentReadingRecord(current)
                .build());
    }
}
