package com.ecovolt.billing.meter;

import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerService;
import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.meter.dto.MeterRequest;
import com.ecovolt.billing.meter.dto.MeterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
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
                .customer(customer)
                .build();
        return MeterResponse.from(meterRepository.save(meter));
    }

    @Transactional(readOnly = true)
    public List<MeterResponse> findAll() {
        return meterRepository.findAll().stream()
                .map(MeterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Meter getMeterOrThrow(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter", "id", id));
    }
}
