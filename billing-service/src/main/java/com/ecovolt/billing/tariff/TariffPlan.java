package com.ecovolt.billing.tariff;

import com.ecovolt.billing.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tariff_plans",
        indexes = @Index(name = "idx_tariff_type_dates", columnList = "tariff_type, effective_from, effective_to"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tariff_type_version", columnNames = {"tariff_type", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tariff_type", nullable = false, length = 32)
    private TariffType type;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "tariffPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TariffSlab> slabs = new ArrayList<>();

    public void replaceSlabs(List<TariffSlab> replacement) {
        slabs.clear();
        replacement.forEach(slab -> {
            slab.setTariffPlan(this);
            slabs.add(slab);
        });
    }
}
