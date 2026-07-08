package com.ecovolt.billing.invoice;

import com.ecovolt.billing.common.http.QueryMethod;
import com.ecovolt.billing.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    private static final String ACCEPT_QUERY = "Accept-Query";
    private static final String FORM_QUERY_MEDIA_TYPE = "application/x-www-form-urlencoded";

    private final InvoiceService invoiceService;
    private final InvoiceGenerationService invoiceGenerationService;

    @PostMapping("/generate/{customerId}")
    @Operation(summary = "Generate invoices for a customer from the latest two readings per meter")
    public ResponseEntity<List<InvoiceResponse>> generate(@PathVariable Long customerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceGenerationService.generateForCustomer(customerId));
    }

    @GetMapping
    @Operation(summary = "List invoices with pagination")
    public Page<InvoiceResponse> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return invoiceService.findAll(pageable);
    }

    @QueryMethod
    @RequestMapping
    @Operation(summary = "Query invoices with pagination")
    public ResponseEntity<Page<InvoiceResponse>> queryAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok()
                .header(ACCEPT_QUERY, FORM_QUERY_MEDIA_TYPE)
                .body(invoiceService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an invoice by id")
    public InvoiceResponse findById(@PathVariable Long id) {
        return invoiceService.findById(id);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Mark an invoice as paid")
    public InvoiceResponse pay(@PathVariable Long id) {
        return invoiceService.pay(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an invoice")
    public InvoiceResponse cancel(@PathVariable Long id) {
        return invoiceService.cancel(id);
    }
}
