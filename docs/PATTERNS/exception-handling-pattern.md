# Exception Handling Pattern

## Description

A three-part system handles all error conditions:

1. **Typed business exceptions** — `RuntimeException` subclasses with no stack-trace
   overhead beyond the message:
   - `ResourceNotFoundException` → HTTP 404
   - `BillingException` → HTTP 422 (business rule violation)
   - `DataIntegrityViolationException` → HTTP 409
   - optimistic locking failures → HTTP 409

2. **Centralised handler** — `@RestControllerAdvice GlobalExceptionHandler` maps each
   exception type to `ApiError` + `ResponseEntity` with the correct HTTP status.
   A catch-all `Exception` handler logs and returns 500 for unexpected errors.

3. **Uniform error payload** — `ApiError` is a `record` with:
   `timestamp, status, error, message, path, fieldErrors (nullable)`.
   Two static factory overloads: `of(…)` without field errors and `of(…, Map fieldErrors)`
   for `MethodArgumentNotValidException`.

## When to use

- Throw `ResourceNotFoundException(resource, field, value)` from any service `findById`-style
  lookup that must return a single entity.
- Throw `BillingException(message)` when a domain/business invariant is violated
  (duplicate key, insufficient data, constraint breach).
- Let database constraint and optimistic-lock exceptions bubble to `GlobalExceptionHandler`
  when the persistence layer is the source of truth.
- Do NOT add new `@ExceptionHandler` methods to `GlobalExceptionHandler` for cases
  already covered by the existing hierarchy — prefer a new subclass of `BillingException`.

## Template — new exception subclass

```java
/**
 * Thrown when {condition}. Mapped to HTTP 422.
 */
public class {Name}Exception extends BillingException {
    public {Name}Exception(String message) {
        super(message);
    }
}
```

## Template — service usage

```java
// 404 pattern
Entity entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("{Entity}", "id", id));

// 422 pattern
if (someBusinessRuleViolated) {
    throw new BillingException("Descriptive message with context: " + value);
}
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/exception/BillingException.java`
- `billing-service/src/main/java/com/ecovolt/billing/exception/ResourceNotFoundException.java`
- `billing-service/src/main/java/com/ecovolt/billing/exception/GlobalExceptionHandler.java`
- `billing-service/src/main/java/com/ecovolt/billing/exception/ApiError.java`
- Usage in `CustomerService.java` (getCustomerOrThrow, line 66)
- Usage in `MeterService.java` (getMeterOrThrow + BillingException, lines 24-28, 46-48)
- Usage in `MeterReadingService.java` (BillingException x3, lines 26-47)
- Usage in `InvoiceGenerationService.java` (BillingException x3, lines 39-63) and `InvoiceService.java` (ResourceNotFoundException, line 30)
