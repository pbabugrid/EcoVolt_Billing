package com.ecovolt.billing.meter;


import com.ecovolt.billing.common.BaseEntity;
import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.reading.MeterReading;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "meters",
        uniqueConstraints = @UniqueConstraint(name = "uk_meter_number", columnNames = "meter_number"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_number", nullable = false, unique = true, updatable = false)
    private String meterNumber;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeterStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default
    @OneToMany(mappedBy = "meter", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeterReading> readings = new ArrayList<>();
}
