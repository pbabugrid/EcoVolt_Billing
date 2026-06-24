package com.ecovolt.billing.reading;


import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.meter.Meter;
import com.ecovolt.billing.meter.MeterService;
import com.ecovolt.billing.reading.dto.MeterReadingRequest;
import com.ecovolt.billing.reading.dto.MeterReadingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;

    @Transactional
    public MeterReadingResponse create(MeterReadingRequest request) {
        Meter meter = meterService.getMeterOrThrow(request.meterId());

        if (meterReadingRepository.existsByMeter_IdAndReadingDate(meter.getId(), request.readingDate())) {
            throw new BillingException("A reading already exists for meter id %d on %s"
                    .formatted(meter.getId(), request.readingDate()));
        }

        meterReadingRepository
                .findFirstByMeter_IdAndReadingDateBeforeOrderByReadingDateDescIdDesc(meter.getId(), request.readingDate())
                .filter(previous -> request.readingValue().compareTo(previous.getReadingValue()) < 0)
                .ifPresent(previous -> {
                    throw new BillingException(
                            "Reading value (%s) is lower than previous reading (%s) from %s"
                                    .formatted(request.readingValue(), previous.getReadingValue(), previous.getReadingDate()));
                });

        meterReadingRepository
                .findFirstByMeter_IdAndReadingDateAfterOrderByReadingDateAscIdAsc(meter.getId(), request.readingDate())
                .filter(next -> request.readingValue().compareTo(next.getReadingValue()) > 0)
                .ifPresent(next -> {
                    throw new BillingException(
                            "Reading value (%s) is higher than next reading (%s) from %s"
                                    .formatted(request.readingValue(), next.getReadingValue(), next.getReadingDate()));
                });

        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .readingDate(request.readingDate())
                .readingValue(request.readingValue())
                .build();
        MeterReading saved = meterReadingRepository.save(reading);
        log.info("event=meter_reading_created readingId={} meterId={} readingDate={}",
                saved.getId(), meter.getId(), saved.getReadingDate());
        return MeterReadingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<MeterReadingResponse> findAll(Pageable pageable) {
        return meterReadingRepository.findAll(pageable).map(MeterReadingResponse::from);
    }
}
