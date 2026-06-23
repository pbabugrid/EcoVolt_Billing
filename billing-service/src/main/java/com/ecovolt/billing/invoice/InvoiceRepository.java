package com.ecovolt.billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByCustomer_IdAndPreviousReadingRecord_IdAndCurrentReadingRecord_Id(
            Long customerId, Long previousReadingId, Long currentReadingId);
}
