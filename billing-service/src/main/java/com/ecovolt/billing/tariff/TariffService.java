package com.ecovolt.billing.tariff;

import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.tariff.dto.TariffPlanRequest;
import com.ecovolt.billing.tariff.dto.TariffPlanResponse;
import com.ecovolt.billing.tariff.dto.TariffSlabRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TariffService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final LocalDate OPEN_ENDED_EFFECTIVE_TO = LocalDate.of(9999, 12, 31);

    private final TariffPlanRepository tariffPlanRepository;

    @Transactional
    public TariffPlanResponse create(TariffPlanRequest request) {
        validatePlan(request);

        if (tariffPlanRepository.existsByTypeAndVersion(request.type(), request.version())) {
            throw new BillingException("Tariff version already exists for %s: %d"
                    .formatted(request.type(), request.version()));
        }

        LocalDate effectiveTo = request.effectiveTo() == null
                ? OPEN_ENDED_EFFECTIVE_TO
                : request.effectiveTo();
        if (tariffPlanRepository.existsActiveOverlap(request.type(), request.effectiveFrom(), effectiveTo)) {
            throw new BillingException("Active tariff effective dates overlap for %s".formatted(request.type()));
        }

        TariffPlan plan = TariffPlan.builder()
                .type(request.type())
                .version(request.version())
                .name(request.name())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .active(true)
                .build();
        plan.replaceSlabs(toSlabs(request.slabs()));
        TariffPlan saved = tariffPlanRepository.save(plan);
        log.info("event=tariff_plan_created tariffPlanId={} tariffType={} tariffVersion={}",
                saved.getId(), saved.getType(), saved.getVersion());
        return TariffPlanResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<TariffPlanResponse> findAll(Pageable pageable) {
        return tariffPlanRepository.findAll(pageable).map(TariffPlanResponse::from);
    }

    @Transactional(readOnly = true)
    public TariffPlanResponse findById(Long id) {
        return TariffPlanResponse.from(getPlanOrThrow(id));
    }

    @Transactional
    public TariffPlanResponse deactivate(Long id, LocalDate effectiveTo) {
        TariffPlan plan = getPlanOrThrow(id);
        if (effectiveTo != null && !effectiveTo.isAfter(plan.getEffectiveFrom())) {
            throw new BillingException("effectiveTo must be after effectiveFrom");
        }
        plan.setEffectiveTo(effectiveTo == null ? LocalDate.now() : effectiveTo);
        plan.setActive(false);
        log.info("event=tariff_plan_deactivated tariffPlanId={} tariffType={} tariffVersion={} effectiveTo={}",
                plan.getId(), plan.getType(), plan.getVersion(), plan.getEffectiveTo());
        return TariffPlanResponse.from(plan);
    }

    @Transactional(readOnly = true)
    public TariffCalculation calculateAmount(TariffType type, BigDecimal unitsConsumed, LocalDate invoiceDate) {
        if (unitsConsumed.signum() < 0) {
            throw new BillingException("unitsConsumed must not be negative");
        }

        TariffPlan plan = tariffPlanRepository.findActiveCandidates(type, invoiceDate).stream()
                .findFirst()
                .orElseThrow(() -> new BillingException("No active tariff found for %s on %s".formatted(type, invoiceDate)));

        BigDecimal amount = calculateSlabAmount(unitsConsumed, plan.getSlabs());
        return new TariffCalculation(amount, plan);
    }

    private TariffPlan getPlanOrThrow(Long id) {
        return tariffPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TariffPlan", "id", id));
    }

    private BigDecimal calculateSlabAmount(BigDecimal unitsConsumed, List<TariffSlab> slabs) {
        BigDecimal total = BigDecimal.ZERO;
        for (TariffSlab slab : slabs.stream().sorted(Comparator.comparing(TariffSlab::getSortOrder)).toList()) {
            BigDecimal slabStart = slab.getFromUnits();
            BigDecimal slabEnd = slab.getToUnits();
            if (unitsConsumed.compareTo(slabStart) <= 0) {
                break;
            }

            BigDecimal billedUntil = slabEnd == null || unitsConsumed.compareTo(slabEnd) < 0
                    ? unitsConsumed
                    : slabEnd;
            BigDecimal slabUnits = billedUntil.subtract(slabStart);
            total = total.add(slabUnits.multiply(slab.getRatePerUnit()));

            if (slabEnd == null || unitsConsumed.compareTo(slabEnd) <= 0) {
                break;
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePlan(TariffPlanRequest request) {
        if (request.effectiveTo() != null && !request.effectiveTo().isAfter(request.effectiveFrom())) {
            throw new BillingException("effectiveTo must be after effectiveFrom");
        }

        List<TariffSlabRequest> slabs = request.slabs().stream()
                .sorted(Comparator.comparing(TariffSlabRequest::fromUnits))
                .toList();
        BigDecimal expectedFrom = ZERO;
        for (int i = 0; i < slabs.size(); i++) {
            TariffSlabRequest slab = slabs.get(i);
            boolean last = i == slabs.size() - 1;

            if (slab.fromUnits().compareTo(expectedFrom) != 0) {
                throw new BillingException("Tariff slabs must be continuous from 0.00");
            }
            if (slab.toUnits() == null && !last) {
                throw new BillingException("Only the last tariff slab may be open-ended");
            }
            if (slab.toUnits() != null && slab.toUnits().compareTo(slab.fromUnits()) <= 0) {
                throw new BillingException("toUnits must be greater than fromUnits");
            }
            expectedFrom = slab.toUnits();
        }
        if (slabs.get(slabs.size() - 1).toUnits() != null) {
            throw new BillingException("Last tariff slab must be open-ended");
        }
    }

    private List<TariffSlab> toSlabs(List<TariffSlabRequest> requests) {
        List<TariffSlabRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(TariffSlabRequest::fromUnits))
                .toList();
        return java.util.stream.IntStream.range(0, sorted.size())
                .mapToObj(index -> TariffSlab.builder()
                        .sortOrder(index + 1)
                        .fromUnits(sorted.get(index).fromUnits())
                        .toUnits(sorted.get(index).toUnits())
                        .ratePerUnit(sorted.get(index).ratePerUnit())
                        .build())
                .toList();
    }
}
