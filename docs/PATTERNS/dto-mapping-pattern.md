# DTO Mapping Pattern

## Description

All data transfer objects are Java `record` types living in the domain's `dto/` sub-package.

**Request records** carry Bean Validation constraints directly on components:
- `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Pattern`, `@PastOrPresent`, `@PositiveOrZero`
- Every constraint includes a human-readable `message` attribute.

**Response records** expose a single static factory method `from(Entity entity)` that
maps from the JPA entity to the record without exposing managed object state beyond
what the HTTP layer needs.

## When to use

For every inbound API request and every API response. Never expose raw JPA entities
from controllers.

## Request template

```java
public record {Domain}Request(

        @NotBlank(message = "{field} is required")
        @Size(max = 120, message = "{field} must not exceed 120 characters")
        String {field},

        @NotNull(message = "{field} is required")
        // ...additional constraint annotations
        {Type} {field2}
) {}
```

## Response template

```java
public record {Domain}Response(
        Long id,
        // ... projection fields
        {Domain}Status status,
        Instant createdAt,
        Instant updatedAt
) {
    public static {Domain}Response from({Domain} entity) {
        return new {Domain}Response(
                entity.getId(),
                // ... map each field
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/customer/dto/CustomerRequest.java` — @NotBlank, @Email, @Pattern, @Size
- `billing-service/src/main/java/com/ecovolt/billing/customer/dto/CustomerResponse.java` — static `from(Customer)`
- `billing-service/src/main/java/com/ecovolt/billing/meter/dto/MeterRequest.java` — @NotNull, @PastOrPresent, @Size
- `billing-service/src/main/java/com/ecovolt/billing/meter/dto/MeterResponse.java` — static `from(Meter)`
- `billing-service/src/main/java/com/ecovolt/billing/reading/dto/MeterReadingRequest.java` — @PositiveOrZero, @PastOrPresent
- `billing-service/src/main/java/com/ecovolt/billing/reading/dto/MeterReadingResponse.java` — static `from(MeterReading)`
- `billing-service/src/main/java/com/ecovolt/billing/invoice/dto/InvoiceResponse.java` — static `from(Invoice)` (read-only; no request DTO for generation)
- `billing-service/src/main/java/com/ecovolt/billing/invoice/dto/InvoiceQueryRequest.java` — JSON body request for QUERY pagination defaults
- `billing-service/src/main/java/com/ecovolt/billing/tariff/dto/TariffPlanRequest.java` — nested slab validation
- `billing-service/src/main/java/com/ecovolt/billing/tariff/dto/TariffPlanResponse.java` — static `from(TariffPlan)`
- `billing-service/src/main/java/com/ecovolt/billing/tariff/dto/TariffSlabRequest.java` — slab band validation
- `billing-service/src/main/java/com/ecovolt/billing/tariff/dto/TariffSlabResponse.java` — static `from(TariffSlab)`
