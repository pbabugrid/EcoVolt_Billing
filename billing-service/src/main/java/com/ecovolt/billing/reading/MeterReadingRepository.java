package com.ecovolt.billing.reading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    /**
     * Returns the two most recent readings for a specific meter, newest first.
     * Ties on date are broken by id (latest persisted first).
     */
    List<MeterReading> findTop2ByMeter_IdOrderByReadingDateDescIdDesc(Long meterId);

    boolean existsByMeter_IdAndReadingDate(Long meterId, LocalDate readingDate);

    Optional<MeterReading> findFirstByMeter_IdAndReadingDateBeforeOrderByReadingDateDescIdDesc(
            Long meterId, LocalDate readingDate);

    Optional<MeterReading> findFirstByMeter_IdAndReadingDateAfterOrderByReadingDateAscIdAsc(
            Long meterId, LocalDate readingDate);
}
