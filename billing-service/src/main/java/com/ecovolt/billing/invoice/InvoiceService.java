package com.ecovolt.billing.invoice;

import com.ecovolt.billing.exception.BillingException;
import com.ecovolt.billing.exception.ResourceNotFoundException;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-side queries for invoices. Generation lives in {@link InvoiceGenerationService}.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        return invoiceRepository.findAll().stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(Long id) {
        return InvoiceResponse.from(getInvoiceOrThrow(id));
    }

    @Transactional
    public InvoiceResponse pay(Long id) {
        Invoice invoice = getInvoiceOrThrow(id);
        transition(invoice, InvoiceStatus.PAID);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice invoice = getInvoiceOrThrow(id);
        transition(invoice, InvoiceStatus.CANCELLED);
        return InvoiceResponse.from(invoice);
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
