package com.ecovolt.billing.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableSanitizer {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    private PageableSanitizer() {
    }

    public static Pageable withDefaultSort(Pageable pageable) {
        Sort sanitizedSort = sanitizeSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizedSort);
    }

    private static Sort sanitizeSort(Sort sort) {
        Sort sanitized = Sort.unsorted();
        for (Sort.Order order : sort) {
            if (isSwaggerPlaceholder(order.getProperty())) {
                continue;
            }
            sanitized = sanitized.and(Sort.by(order));
        }
        return sanitized.isSorted() ? sanitized : DEFAULT_SORT;
    }

    private static boolean isSwaggerPlaceholder(String property) {
        return "string".equals(property) || "[\"string\"]".equals(property);
    }
}
