package com.ecovolt.billing.invoice.dto;

import com.ecovolt.billing.common.PageableSanitizer;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public record InvoiceQueryRequest(
        @Min(0) Integer page,
        @Min(1) Integer size,
        List<String> sort) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    public Pageable toPageable() {
        return PageableSanitizer.withDefaultSort(PageRequest.of(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                toSort()));
    }

    private Sort toSort() {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }

        Sort parsed = Sort.unsorted();
        for (String value : sort) {
            if (value == null || value.isBlank()) {
                continue;
            }
            parsed = parsed.and(toOrder(value));
        }
        return parsed;
    }

    private Sort toOrder(String value) {
        String[] parts = value.split(",");
        String property = parts[0].trim();
        if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
            return Sort.by(Sort.Direction.DESC, property);
        }
        return Sort.by(Sort.Direction.ASC, property);
    }
}
