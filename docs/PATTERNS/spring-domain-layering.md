# Spring Domain Layering

## Description

Every domain module follows a four-layer vertical slice:
`Entity → Repository → Service → Controller`, with a nested `dto/` sub-package
for Request/Response types. Each layer has a single, narrow responsibility. This
structure makes it trivial to add a new domain by replicating the same file set.

## When to use

Whenever a new bounded-context resource is introduced (e.g. payment, tariff plan,
meter type). Do NOT apply to cross-cutting concerns or infrastructure utilities.

## Structure

```
{domain}/
  {Domain}.java              # JPA entity
  {Domain}Status.java        # optional enum for lifecycle state
  {Domain}Repository.java    # Spring Data JPA repository interface
  {Domain}Service.java       # business logic + transaction boundary
  {Domain}Controller.java    # REST endpoint, delegates to service
  dto/
    {Domain}Request.java     # input record with Bean Validation
    {Domain}Response.java    # output record with static from(Entity)
```

## Extension points

- Add `{Domain}GenerationService.java` for complex creation workflows (see `invoice/`).
- Add query methods directly on `{Domain}Repository` for domain-specific finders.
- Status enum (e.g., `ACTIVE`, `INACTIVE`) follows `{Domain}Status` naming convention.

## Evidence

### customer

- Entity: `billing-service/src/main/java/com/ecovolt/billing/customer/Customer.java`
- Service: `CustomerService.java`
- Controller: `CustomerController.java`
- DTOs: `dto/CustomerRequest.java`, `dto/CustomerResponse.java`

### meter

- Entity: `billing-service/src/main/java/com/ecovolt/billing/meter/Meter.java`
- Service: `MeterService.java`
- Controller: `MeterController.java`
- DTOs: `dto/MeterRequest.java`, `dto/MeterResponse.java`

### reading

- Entity: `billing-service/src/main/java/com/ecovolt/billing/reading/MeterReading.java`
- Service: `MeterReadingService.java`
- Controller: `MeterReadingController.java`
- DTOs: `dto/MeterReadingRequest.java`, `dto/MeterReadingResponse.java`

### invoice

- Entity: `billing-service/src/main/java/com/ecovolt/billing/invoice/Invoice.java`
- Services: `InvoiceService.java`, `InvoiceGenerationService.java`
- Controller: `InvoiceController.java`
- DTOs: `dto/InvoiceResponse.java`

### tariff

- Entities: `billing-service/src/main/java/com/ecovolt/billing/tariff/TariffPlan.java`, `TariffSlab.java`
- Service: `TariffService.java`
- Controller: `TariffController.java`
- DTOs: `dto/TariffPlanRequest.java`, `dto/TariffPlanResponse.java`, `dto/TariffSlabRequest.java`, `dto/TariffSlabResponse.java`
