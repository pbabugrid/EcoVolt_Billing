package com.ecovolt.billing.customer;


import com.ecovolt.billing.customer.dto.CustomerRequest;
import com.ecovolt.billing.customer.dto.CustomerResponse;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.invoice.InvoiceRepository;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import com.ecovolt.billing.meter.MeterRepository;
import com.ecovolt.billing.meter.dto.MeterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final MeterRepository meterRepository;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .customerNumber(generateCustomerNumber())
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .status(CustomerStatus.ACTIVE)
                .build();
        Customer saved = customerRepository.save(customer);
        log.info("event=customer_created customerId={} customerNumber={}", saved.getId(), saved.getCustomerNumber());
        return CustomerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.from(getCustomerOrThrow(id));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomerOrThrow(id);
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        log.info("event=customer_updated customerId={} customerNumber={}", customer.getId(), customer.getCustomerNumber());
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getCustomerOrThrow(id);
        customer.setStatus(CustomerStatus.INACTIVE);
        log.info("event=customer_deactivated customerId={} customerNumber={}", customer.getId(), customer.getCustomerNumber());
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findInvoices(Long customerId, Pageable pageable) {
        getCustomerOrThrow(customerId);
        return invoiceRepository.findByCustomer_Id(customerId, pageable).map(InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MeterResponse> findMeters(Long customerId, Pageable pageable) {
        getCustomerOrThrow(customerId);
        return meterRepository.findByCustomer_Id(customerId, pageable).map(MeterResponse::from);
    }

    /**
     * Shared lookup used by other modules (meter, invoice) to resolve a managed Customer.
     */
    @Transactional(readOnly = true)
    public Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    private String generateCustomerNumber() {
        String candidate;
        do {
            candidate = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (customerRepository.existsByCustomerNumber(candidate));
        return candidate;
    }
}
