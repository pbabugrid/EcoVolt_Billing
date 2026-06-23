package com.ecovolt.billing.customer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerNumber(String customerNumber);
}
