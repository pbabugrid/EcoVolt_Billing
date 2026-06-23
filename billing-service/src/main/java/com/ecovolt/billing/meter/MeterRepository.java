package com.ecovolt.billing.meter;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Meter> findByCustomer_IdOrderByIdAsc(Long customerId);
}
