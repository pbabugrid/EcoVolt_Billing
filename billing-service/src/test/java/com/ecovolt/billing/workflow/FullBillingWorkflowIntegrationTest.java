package com.ecovolt.billing.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FullBillingWorkflowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Full REST billing workflow creates customer, meter, readings, invoice, and paid invoice")
    void fullRestBillingWorkflow_createsAndPaysInvoice() throws Exception {
        /*
         * Scenario:
         * Client -> Customer API: create customer
         * Client -> Meter API: assign residential meter
         * Client -> Reading API: submit previous and current readings
         * Client -> Invoice API: generate invoice from latest two readings
         * Client -> Invoice API: pay generated invoice
         */
        Long customerId = createCustomer();
        Long meterId = createMeter(customerId);
        Long previousReadingId = createReading(meterId, "2024-01-01", "100.00");
        Long currentReadingId = createReading(meterId, "2024-02-01", "160.00");

        String invoiceBody = mockMvc.perform(post("/api/invoices/generate/{customerId}", customerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[0].previousReadingId").value(previousReadingId))
                .andExpect(jsonPath("$[0].currentReadingId").value(currentReadingId))
                .andExpect(jsonPath("$[0].previousReading").value(100.00))
                .andExpect(jsonPath("$[0].currentReading").value(160.00))
                .andExpect(jsonPath("$[0].unitsConsumed").value(60.00))
                .andExpect(jsonPath("$[0].amount").value(300.00))
                .andExpect(jsonPath("$[0].status").value("GENERATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long invoiceId = objectMapper.readTree(invoiceBody).get(0).get("id").asLong();

        mockMvc.perform(post("/api/invoices/{id}/pay", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    private Long createCustomer() throws Exception {
        String body = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Workflow Customer",
                                  "email": "workflow@ecovolt.test",
                                  "phone": "+15550000002",
                                  "address": "Workflow address"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode customer = objectMapper.readTree(body);
        assertThat(customer.get("customerNumber").asText()).startsWith("CUST-");
        return customer.get("id").asLong();
    }

    private Long createMeter(Long customerId) throws Exception {
        String body = mockMvc.perform(post("/api/meters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": %d,
                                  "meterNumber": "MTR-WORKFLOW-001",
                                  "installationDate": "2024-01-01",
                                  "tariffType": "RESIDENTIAL"
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.meterNumber").value("MTR-WORKFLOW-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createReading(Long meterId, String readingDate, String readingValue) throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "meterId": %d,
                                  "readingDate": "%s",
                                  "readingValue": %s
                                }
                                """.formatted(meterId, readingDate, readingValue)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meterId").value(meterId))
                .andExpect(jsonPath("$.readingDate").value(readingDate))
                .andExpect(jsonPath("$.readingValue").value(Double.parseDouble(readingValue)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
