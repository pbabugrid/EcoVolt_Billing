package com.ecovolt.billing.customer.dto;



import com.ecovolt.billing.customer.Customer;
import com.ecovolt.billing.customer.CustomerStatus;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String customerNumber,
        String name,
        String email,
        String phone,
        String address,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getCustomerNumber(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getAddress(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
