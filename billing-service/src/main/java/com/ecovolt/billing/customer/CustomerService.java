package com.ecovolt.billing.customer;


import com.ecovolt.billing.customer.dto.CustomerRequest;
import com.ecovolt.billing.customer.dto.CustomerResponse;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

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
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
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
        // Managed entity: changes are flushed on transaction commit (dirty checking).
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getCustomerOrThrow(id);
        customer.setStatus(CustomerStatus.INACTIVE);
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
