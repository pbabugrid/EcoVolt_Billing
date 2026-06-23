package com.ecovolt.billing.reading;

import com.ecovolt.billing.common.BaseEntity;
import com.ecovolt.billing.meter.Meter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "meter_readings",
        indexes = @Index(name = "idx_reading_meter_date", columnList = "meter_id, reading_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_reading_meter_date", columnNames = {"meter_id", "reading_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "reading_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal readingValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;
}
