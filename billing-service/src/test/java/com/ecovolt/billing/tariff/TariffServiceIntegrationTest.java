package com.ecovolt.billing.tariff;

import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.tariff.dto.TariffPlanRequest;
import com.ecovolt.billing.tariff.dto.TariffSlabRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TariffServiceIntegrationTest {

    @Autowired TariffService tariffService;
    @Autowired TariffPlanRepository tariffPlanRepository;

    @Test
    @DisplayName("Slab pricing calculates amount from active residential tariff")
    void calculateAmount_usesActiveSlabs() {
        TariffCalculation calculation = tariffService.calculateAmount(
                TariffType.RESIDENTIAL,
                new BigDecimal("150.00"),
                LocalDate.of(2024, 6, 1));

        assertThat(calculation.tariffPlan().getType()).isEqualTo(TariffType.RESIDENTIAL);
        assertThat(calculation.tariffPlan().getVersion()).isEqualTo(1);
        assertThat(calculation.amount()).isEqualByComparingTo("850.00");
    }

    @Test
    @DisplayName("Creating overlapping active tariff version is rejected")
    void create_overlappingActiveWindow_throws422() {
        TariffPlanRequest request = request(TariffType.RESIDENTIAL, 2, LocalDate.of(2024, 6, 1), null);

        assertThatThrownBy(() -> tariffService.create(request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    @DisplayName("New tariff version becomes active after previous version is closed")
    void create_afterClosingPreviousVersion_succeeds() {
        TariffPlan plan = tariffPlanRepository.findActiveCandidates(
                        TariffType.COMMERCIAL, LocalDate.of(2024, 6, 1))
                .getFirst();
        plan.setEffectiveTo(LocalDate.of(2025, 1, 1));
        plan.setActive(false);

        tariffService.create(request(TariffType.COMMERCIAL, 2, LocalDate.of(2025, 1, 1), null));

        TariffCalculation calculation = tariffService.calculateAmount(
                TariffType.COMMERCIAL,
                new BigDecimal("150.00"),
                LocalDate.of(2025, 6, 1));

        assertThat(calculation.tariffPlan().getVersion()).isEqualTo(2);
        assertThat(calculation.amount()).isEqualByComparingTo("1100.00");
    }

    @Test
    @DisplayName("Non-continuous slabs are rejected")
    void create_nonContinuousSlabs_throws422() {
        TariffPlanRequest request = new TariffPlanRequest(
                TariffType.INDUSTRIAL,
                2,
                "Industrial gap",
                LocalDate.of(2030, 1, 1),
                null,
                List.of(
                        new TariffSlabRequest(new BigDecimal("0.00"), new BigDecimal("50.00"), new BigDecimal("1.00")),
                        new TariffSlabRequest(new BigDecimal("60.00"), null, new BigDecimal("2.00"))));

        assertThatThrownBy(() -> tariffService.create(request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("continuous");
    }

    private static TariffPlanRequest request(TariffType type, int version, LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new TariffPlanRequest(
                type,
                version,
                type.name() + " v" + version,
                effectiveFrom,
                effectiveTo,
                List.of(
                        new TariffSlabRequest(new BigDecimal("0.00"), new BigDecimal("100.00"), new BigDecimal("6.00")),
                        new TariffSlabRequest(new BigDecimal("100.00"), null, new BigDecimal("10.00"))));
    }
}
