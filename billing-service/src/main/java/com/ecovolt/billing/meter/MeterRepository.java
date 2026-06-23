package com.ecovolt.billing.meter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);
}
