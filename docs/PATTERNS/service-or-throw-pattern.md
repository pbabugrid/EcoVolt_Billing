# Service OrThrow Lookup Pattern

## Description

Services expose a public `getXxxOrThrow(Long id)` method that:
1. Fetches an entity by PK from its repository.
2. Returns the managed JPA entity on success.
3. Throws `ResourceNotFoundException` on miss.

This method is callable by **other services** in cross-domain operations, keeping
foreign-key resolution consistent and avoiding duplicated `orElseThrow` boilerplate.

## When to use

Whenever a service needs to resolve a cross-domain entity reference (e.g. creating a
Meter requires a valid Customer). Always call the owning service's `getXxxOrThrow`
instead of injecting the foreign repository directly.

## Template

```java
// In {Domain}Service:
@Transactional(readOnly = true)
public {Domain} get{Domain}OrThrow(Long id) {
    return {domain}Repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("{Domain}", "id", id));
}
```

## Caller pattern

```java
// In a dependent service (constructor-injected):
private final {OwnerDomain}Service {ownerDomain}Service;

{Domain} entity = {ownerDomain}Service.get{OwnerDomain}OrThrow(request.{ownerId}());
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/customer/CustomerService.java`
  — `getCustomerOrThrow(Long id)` (line 66); called by `MeterService` and `InvoiceGenerationService`
- `billing-service/src/main/java/com/ecovolt/billing/meter/MeterService.java`
  — `getMeterOrThrow(Long id)` (line 45); called by `MeterReadingService`
