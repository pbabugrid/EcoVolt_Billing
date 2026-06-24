package com.ecovolt.billing.invoice;

import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side queries for invoices. Generation lives in {@link InvoiceGenerationService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findAll(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(InvoiceResponse::from);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return InvoiceResponse.from(getInvoiceOrThrow(id));
    }

    @Transactional
    public InvoiceResponse pay(Long id) {
        Invoice invoice = getInvoiceOrThrow(id);
        transition(invoice, InvoiceStatus.PAID);
        log.info("event=invoice_paid invoiceId={} invoiceNumber={}", invoice.getId(), invoice.getInvoiceNumber());
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = getInvoiceOrThrow(id);
        transition(invoice, InvoiceStatus.CANCELLED);
        log.info("event=invoice_cancelled invoiceId={} invoiceNumber={}", invoice.getId(), invoice.getInvoiceNumber());
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findByCustomerId(Long customerId, Pageable pageable) {
        return invoiceRepository.findByCustomer_Id(customerId, pageable).map(InvoiceResponse::from);
    }

    private Invoice getInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
    }

    private void transition(Invoice invoice, InvoiceStatus targetStatus) {
        if (!isValidTransition(invoice.getStatus(), targetStatus)) {
            throw new BillingException("Invoice %s cannot transition from %s to %s"
                    .formatted(invoice.getInvoiceNumber(), invoice.getStatus(), targetStatus));
        }
        invoice.setStatus(targetStatus);
    }

    private boolean isValidTransition(InvoiceStatus currentStatus, InvoiceStatus targetStatus) {
        return (currentStatus == InvoiceStatus.GENERATED || currentStatus == InvoiceStatus.OVERDUE)
                && (targetStatus == InvoiceStatus.PAID || targetStatus == InvoiceStatus.CANCELLED);
    }
}
