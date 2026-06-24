package com.ecovolt.billing.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
            Long customerId, Long previousReadingId, Long currentReadingId);

    Page<Invoice> findByCustomer_Id(Long customerId, Pageable pageable);
}
