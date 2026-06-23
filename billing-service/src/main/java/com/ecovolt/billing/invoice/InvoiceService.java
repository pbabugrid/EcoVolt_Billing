package com.ecovolt.billing.invoice;

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
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return InvoiceResponse.from(invoice);
    }
}
