package com.ecovolt.billing.tariff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TariffControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired TariffPlanRepository tariffPlanRepository;

    @Test
    @DisplayName("Tariff list API returns seeded tariff plans")
    void findAll_returnsSeededPlans() throws Exception {
        mockMvc.perform(get("/api/tariff-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].slabs").isArray());
    }

    @Test
    @DisplayName("Create tariff API validates overlapping effective dates")
    void create_overlappingWindow_returns422() throws Exception {
        mockMvc.perform(post("/api/tariff-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "RESIDENTIAL",
                                  "version": 2,
                                  "name": "Residential overlap",
                                  "effectiveFrom": "2024-06-01",
                                  "slabs": [
                                    {"fromUnits": 0.00, "toUnits": 100.00, "ratePerUnit": 6.00},
                                    {"fromUnits": 100.00, "ratePerUnit": 10.00}
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Deactivate API closes an active tariff version")
    void deactivate_closesEffectiveWindow() throws Exception {
        Long id = tariffPlanRepository.findActiveCandidates(TariffType.INDUSTRIAL, LocalDate.of(2024, 6, 1))
                .getFirst()
                .getId();

        mockMvc.perform(put("/api/tariff-plans/{id}/deactivate", id)
                        .param("effectiveTo", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.effectiveTo").value("2025-01-01"));
    }
}
