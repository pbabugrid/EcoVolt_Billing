package com.ecovolt.billing.exception;

/**
 * Thrown when a business/billing rule is violated (e.g. insufficient readings,
 * negative consumption, duplicate identifiers). Mapped to HTTP 422.
 */
public class BillingException extends RuntimeException {

    public BillingException(String message) {
        super(message);
    }
}
