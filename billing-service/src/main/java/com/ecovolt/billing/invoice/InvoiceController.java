package com.ecovolt.billing.invoice;

import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice generation and retrieval")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceGenerationService invoiceGenerationService;

    @PostMapping("/generate/{customerId}")
    @Operation(summary = "Generate an invoice for a customer from the latest two readings")
    public ResponseEntity<InvoiceResponse> generate(@PathVariable Long customerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceGenerationService.generateForCustomer(customerId));
    }

    @GetMapping
    @Operation(summary = "List all invoices")
    public List<InvoiceResponse> findAll() {
        return invoiceService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an invoice by id")
    public InvoiceResponse findById(@PathVariable Long id) {
        return invoiceService.findById(id);
    }
}
