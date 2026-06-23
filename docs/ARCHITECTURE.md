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
- `invoice`: invoice entity, status enum, repository, read service, generation service, controller, and response DTO.
- `tariff`: flat-rate tariff calculation service.
- `exception`: typed domain exceptions and centralized REST error handling.
- `config`: OpenAPI metadata configuration.

## REST API Surface

- `POST /api/customers`, `GET /api/customers`, `GET /api/customers/{id}`, `PUT /api/customers/{id}`, `DELETE /api/customers/{id}`.
- `POST /api/meters`, `GET /api/meters`.
- `POST /api/readings`, `GET /api/readings`.
- `POST /api/invoices/generate/{customerId}`, `GET /api/invoices`, `GET /api/invoices/{id}`.
- POST endpoints return `201 Created`; delete returns `204 No Content`; read endpoints return DTOs directly.

## Domain Relationships

- Customer has many meters.
- Customer has many invoices.
- Meter belongs to one customer and has many readings.
- Meter reading belongs to one meter.
- Invoice belongs to one customer and references the previous and current meter reading records used to generate it.

## Persistence Design

- JPA entities use generated numeric primary keys.
- Business uniqueness is enforced for customer number, meter number, invoice number, meter/date reading pairs, and invoice source-reading pairs.
- Entities extend `BaseEntity` for audit timestamps.
- Relationships are lazy-loaded; child collections use cascade behavior where modeled.
- Schema evolution currently uses Hibernate update mode in local runtime configuration.

## Validation And Business Rules

- Request DTO records use Jakarta Bean Validation for required fields, email format, phone format, length limits, dates not in the future, and non-negative reading values.
- Customer creation assigns an active status and generated customer number.
- Meter registration requires an existing customer and a unique meter number; meters start active.
- Reading capture rejects duplicate meter/date readings and rejects values that would break chronological monotonicity relative to adjacent readings.
- Target invoice generation requires at least two readings from the same meter, rejects duplicate invoices for the same source readings, rejects negative consumption, calculates amount from tariff, and creates invoices with generated status.
- Current source appears to select the latest two readings across a customer; track same-meter alignment in `docs/TODO.md`.
- Tariff calculation is currently a flat rate of 5 currency units per consumption unit, rounded to two decimals.

## Error Handling

- Missing resources throw `ResourceNotFoundException` and map to HTTP 404.
- Business-rule violations throw `BillingException` and map to HTTP 422.
- request validation failures map to HTTP 400 with field-level errors.
- Unexpected exceptions map to HTTP 500 with a generic message.
- Error responses use the shared `ApiError` record.

## Testing Architecture

- Test framework: Spring Boot Test with JUnit Platform.
- Current test coverage is a single application context smoke test.
- No source-evidenced unit, controller, repository, invoice-generation, or validation scenario tests are present.

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
