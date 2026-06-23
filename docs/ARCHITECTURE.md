# ARCHITECTURE

This document captures technical architecture for the workspace.
Content: modules, APIs, persistence, validation, errors, testing, build/runtime, and technical constraints.
Style: concise, evidence-grounded, grep-friendly. Business context belongs in `docs/CONTEXT.md`; file layout belongs in `docs/CODEMAP.md`.

## Workspace Architecture

- Workspace root contains Rosetta documentation, workflow state, reference-source area, and one Spring Boot application under `billing-service`.
- `billing-service` is a Java Spring Boot REST API with a vertical-slice package layout under `com.ecovolt.billing`.
- See `docs/CODEMAP.md` for complete file inventory and `docs/TECHSTACK.md` for version details.

## Runtime Architecture

- Application entry point: `BillingServiceApplication`.
- Runtime framework: Spring Boot Web MVC with embedded server defaults.
- API documentation: springdoc OpenAPI / Swagger UI configured by `OpenApiConfig`.
- Persistence: Spring Data JPA / Hibernate against an H2 in-memory database for local development and tests.
- Monitoring category: Spring Boot Actuator dependency is present.

## Module Architecture

- `common`: shared `BaseEntity` audit fields for creation and update timestamps.
- `customer`: customer entity, status enum, repository, service, controller, and DTOs.
- `meter`: meter entity, status enum, repository, service, controller, and DTOs.
- `reading`: meter reading entity, repository, service, controller, and DTOs.
- `invoice`: invoice entity, status enum, repository, lifecycle/read service, generation service, controller, and response DTO; invoices retain tariff plan/type/version snapshot fields.
- `tariff`: versioned tariff plans, ordered slabs, repository, service, controller, and DTOs.
- `exception`: typed domain exceptions and centralized REST error handling.
- `config`: OpenAPI metadata configuration.

## REST API Surface

- `POST /api/customers`, `GET /api/customers`, `GET /api/customers/{id}`, `PUT /api/customers/{id}`, `DELETE /api/customers/{id}`.
- `POST /api/meters`, `GET /api/meters`.
- `POST /api/readings`, `GET /api/readings`.
- `POST /api/invoices/generate/{customerId}`, `GET /api/invoices`, `GET /api/invoices/{id}`, `POST /api/invoices/{id}/pay`, `POST /api/invoices/{id}/cancel`.
- `POST /api/tariff-plans`, `GET /api/tariff-plans`, `GET /api/tariff-plans/{id}`, `PUT /api/tariff-plans/{id}/deactivate`.
- Invoice generation returns `201 Created`; customer delete/deactivate returns `204 No Content`; read and invoice lifecycle endpoints return DTOs directly.

## Domain Relationships

- Customer has many meters.
- Customer has many invoices.
- Meter belongs to one customer, has many readings, and carries a tariff type (`RESIDENTIAL`, `COMMERCIAL`, or `INDUSTRIAL`).
- Meter reading belongs to one meter.
- Invoice belongs to one customer and references the previous and current meter reading records used to generate it.
- Tariff plan has many ordered tariff slabs and is selected by tariff type plus invoice generation date.

## Persistence Design

- JPA entities use generated numeric primary keys.
- Business uniqueness is enforced for customer number, meter number, invoice number, meter/date reading pairs, and invoice source-reading pairs.
- Entities extend `BaseEntity` for audit timestamps.
- Relationships are lazy-loaded; child collections use cascade behavior where modeled.
- Schema evolution uses Flyway SQL migrations; Hibernate validates the schema at runtime.

## Validation And Business Rules

- Request DTO records use Jakarta Bean Validation for required fields, email format, phone format, length limits, dates not in the future, and non-negative reading values.
- Customer creation assigns an active status and generated customer number.
- Customer deletion is a safe-retention operation: `DELETE /api/customers/{id}` transitions the customer to `INACTIVE` instead of physically deleting the row, preserving meters, invoices, and audit history.
- Meter registration requires an existing customer and a unique meter number; meters start active.
- Reading capture rejects duplicate meter/date readings and rejects values that would break chronological monotonicity relative to adjacent readings.
- Target invoice generation requires at least two readings from the same meter, rejects duplicate invoices for the same source readings, rejects negative consumption, calculates amount from tariff, and creates invoices with generated status.
- Invoice generation iterates all meters for a customer under a write lock; each meter with at least two readings generates one invoice using its latest two readings ordered by `readingDate DESC, id DESC`. Duplicate source-reading pairs are skipped silently; if no new invoice remains after skipping duplicates, a 422 is returned.
- `POST /api/invoices/generate/{customerId}` returns `List<InvoiceResponse>` (HTTP 201) with one entry per newly generated invoice.
- Tariff plan creation rejects duplicate type/version pairs, invalid effective windows, overlapping active effective windows, non-continuous slabs, and closed final slabs.
- Invoice generation calculates amount from the active tariff for the meter tariff type and generated date, using ordered slab bands and two-decimal rounding.
- Invoice lifecycle transitions allow `GENERATED` or `OVERDUE` invoices to become `PAID` or `CANCELLED`; `PAID` and `CANCELLED` are terminal for pay/cancel APIs and invalid transitions return 422.

## Error Handling

- Missing resources throw `ResourceNotFoundException` and map to HTTP 404.
- Business-rule violations throw `BillingException` and map to HTTP 422.
- request validation failures map to HTTP 400 with field-level errors.
- Unexpected exceptions map to HTTP 500 with a generic message.
- Error responses use the shared `ApiError` record.

## Testing Architecture

- Testing Architecture: test coverage includes a context smoke test, unit tests for invoice generation and reading validation rules, tariff service/controller integration tests, and integration tests for customer retention, invoice lifecycle transitions, multi-meter invoice generation, duplicate replay, active-tariff calculation, and REST response/error mapping — all running against H2 in-memory.

## Build Architecture

- Build tool: Gradle Wrapper.
- Java toolchain in build file: Java 25.
- Java 25 is the intended target for this workspace.
- Framework dependency management: Spring Boot and Spring dependency-management plugins.
- Dependency source: Maven Central.
- Lombok is used as compile-time code generation.

## Patterns

- Documented recurring patterns live under `docs/PATTERNS/`.
- Current pattern inventory includes vertical slice layering, REST controller conventions, JPA entity conventions, DTO mapping, exception handling, cross-domain service lookup, and unique business-key generation.
