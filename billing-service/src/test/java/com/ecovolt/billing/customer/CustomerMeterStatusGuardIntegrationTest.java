package com.ecovolt.billing.customer;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerMeterStatusGuardIntegrationTest {

    @Autowired CustomerRepository customerRepository;
    @Autowired MeterRepository meterRepository;
    @Autowired MeterReadingRepository meterReadingRepository;
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Current behavior allows invoice generation for inactive customers")
    void invoiceGeneration_documentsCurrentPermissiveInactiveCustomerBehavior() throws Exception {
        /*
         * Current permissive flow:
         * Client -> Invoice API: generate invoice for INACTIVE customer
         * Invoice API -> Meter/Reading repositories: use eligible meter readings
         * Invoice API -> Client: 201 Created
         */
        Customer inactiveCustomer = persistCustomer("CUST-INACTIVE-GUARD", CustomerStatus.INACTIVE);
        Meter meter = persistMeter(inactiveCustomer, "MTR-INACTIVE-GUARD", MeterStatus.ACTIVE);
        persistReading(meter, "2024-01-01", "100.00");
        persistReading(meter, "2024-02-01", "140.00");

        mockMvc.perform(post("/api/invoices/generate/{customerId}", inactiveCustomer.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].customerId").value(inactiveCustomer.getId()))
                .andExpect(jsonPath("$[0].status").value("GENERATED"));
    }

    @Test
    @DisplayName("Current behavior allows invoice generation for non-active meters")
    void invoiceGeneration_documentsCurrentPermissiveNonActiveMeterBehavior() throws Exception {
        Customer customer = persistCustomer("CUST-METER-GUARD", CustomerStatus.ACTIVE);
        Meter faultyMeter = persistMeter(customer, "MTR-FAULTY-GUARD", MeterStatus.FAULTY);
        persistReading(faultyMeter, "2024-01-01", "200.00");
        persistReading(faultyMeter, "2024-02-01", "260.00");

        mockMvc.perform(post("/api/invoices/generate/{customerId}", customer.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].customerId").value(customer.getId()))
                .andExpect(jsonPath("$[0].unitsConsumed").value(60.00))
                .andExpect(jsonPath("$[0].status").value("GENERATED"));
    }

    private Customer persistCustomer(String customerNumber, CustomerStatus status) {
        return customerRepository.save(Customer.builder()
                .customerNumber(customerNumber)
                .name(customerNumber)
                .email(customerNumber.toLowerCase() + "@ecovolt.test")
                .status(status)
                .build());
    }

    private Meter persistMeter(Customer customer, String meterNumber, MeterStatus status) {
        return meterRepository.save(Meter.builder()
                .meterNumber(meterNumber)
                .installationDate(LocalDate.of(2024, 1, 1))
                .status(status)
                .customer(customer)
                .build());
    }

    private MeterReading persistReading(Meter meter, String date, String value) {
        return meterReadingRepository.save(MeterReading.builder()
                .meter(meter)
                .readingDate(LocalDate.parse(date))
                .readingValue(new BigDecimal(value))
                .build());
    }
}
