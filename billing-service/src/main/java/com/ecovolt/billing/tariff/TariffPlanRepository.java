package com.ecovolt.billing.tariff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {

    boolean existsByTypeAndVersion(TariffType type, Integer version);

    @Query("""
            select tp from TariffPlan tp
            where tp.type = :type
              and tp.active = true
              and tp.effectiveFrom <= :date
              and (tp.effectiveTo is null or tp.effectiveTo > :date)
            order by tp.version desc
            """)
    List<TariffPlan> findActiveCandidates(@Param("type") TariffType type, @Param("date") LocalDate date);

    @Query("""
            select count(tp) > 0 from TariffPlan tp
            where tp.type = :type
              and tp.active = true
              and tp.effectiveFrom < :effectiveTo
              and (tp.effectiveTo is null or tp.effectiveTo > :effectiveFrom)
            """)
    boolean existsActiveOverlap(
            @Param("type") TariffType type,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);
}
