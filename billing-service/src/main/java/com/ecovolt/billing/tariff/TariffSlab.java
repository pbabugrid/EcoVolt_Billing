package com.ecovolt.billing.tariff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_slabs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "from_units", nullable = false, precision = 12, scale = 2)
    private BigDecimal fromUnits;

    @Column(name = "to_units", precision = 12, scale = 2)
    private BigDecimal toUnits;

    @Column(name = "rate_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    private TariffPlan tariffPlan;

    @Version
    @Column(name = "opt_lock", nullable = false)
    private Long optLock;
}
