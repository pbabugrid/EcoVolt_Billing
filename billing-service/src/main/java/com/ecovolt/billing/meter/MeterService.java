package com.ecovolt.billing.meter;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerService;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.meter.dto.MeterRequest;
import com.ecovolt.billing.meter.dto.MeterResponse;
import com.ecovolt.billing.tariff.TariffType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterService {

    private final MeterRepository meterRepository;
    private final CustomerService customerService;

    @Transactional
    public MeterResponse create(MeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.meterNumber())) {
            throw new BillingException("Meter number already exists: " + request.meterNumber());
        }
        Customer customer = customerService.getCustomerOrThrow(request.customerId());
        Meter meter = Meter.builder()
                .meterNumber(request.meterNumber())
                .installationDate(request.installationDate())
                .status(MeterStatus.ACTIVE)
                .tariffType(request.tariffType() == null ? TariffType.RESIDENTIAL : request.tariffType())
                .customer(customer)
                .build();
        Meter saved = meterRepository.save(meter);
        log.info("event=meter_created meterId={} meterNumber={} customerId={}",
                saved.getId(), saved.getMeterNumber(), customer.getId());
        return MeterResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<MeterResponse> findAll(Pageable pageable) {
        return meterRepository.findAll(pageable).map(MeterResponse::from);
    }

    @Transactional(readOnly = true)
    public MeterResponse findById(Long id) {
        return MeterResponse.from(getMeterOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Meter getMeterOrThrow(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", id));
    }
}
