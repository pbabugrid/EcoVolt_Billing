# IMPLEMENTATION

This document records implementation baseline and implementation change history.
Content: brief baseline first, then dated change-log entries. Detailed architecture belongs in `docs/ARCHITECTURE.md`.
Style: concise, grep-friendly, and non-duplicative.

## Baseline — 2026-06-23 — EcoVolt billing service

- Single Spring Boot service under `billing-service`.
- Implemented modules: customer, meter, meter reading, invoice generation/retrieval, tariff calculation, OpenAPI config, shared exceptions, and base entity auditing.
- API style: REST controllers with DTO records and Jakarta validation.
- Persistence style: JPA entities and Spring Data repositories.
- Testing baseline at initialization: one Spring Boot context-load smoke test; current test coverage is tracked in dated entries below.
- Build baseline: Gradle Wrapper with Java toolchain 25.
- See `docs/ARCHITECTURE.md`, `docs/TECHSTACK.md`, `docs/CODEMAP.md`, `docs/DEPENDENCIES.md`, and `docs/PATTERNS/INDEX.md` for details.

## 2026-06-24 — Invoice generation hardened (same-meter readings + duplicate prevention + tests)

- **Bug fixed**: `InvoiceGenerationService` previously used a customer-wide latest-two reading selector, which could pair readings from different meters. Service now iterates each customer meter individually.
- **MeterRepository**: added `findByCustomer_IdOrderByIdAsc(Long customerId)` with a pessimistic write lock to serialize customer invoice generation across the customer's meters.
- **MeterReadingRepository**: added `findTop2ByMeter_IdOrderByReadingDateDescIdDesc(Long meterId)` for per-meter top-2 selection ordered by `readingDate DESC, id DESC`.
- **InvoiceGenerationService.generateForCustomer** signature changed from `InvoiceResponse` to `List<InvoiceResponse>`. One invoice is generated per eligible meter (≥ 2 readings). Duplicate source-reading pairs are silently skipped; 422 thrown if no eligible meter has two readings, or if all eligible pairs are already invoiced.
- **InvoiceController.generate** updated to return `ResponseEntity<List<InvoiceResponse>>`.
- **Tests added**: `InvoiceGenerationServiceTest` (Mockito unit tests covering single-meter, multi-meter, duplicate, no-readings, negative consumption scenarios), `MeterReadingServiceTest` (Mockito unit tests covering duplicate date and monotonicity validation), and `InvoiceGenerationIntegrationTest` (Spring Boot `@Transactional` integration tests covering same-meter ordering, multi-meter generation, one-reading meter skip, duplicate replay, partial-duplicate multi-meter scenarios, and REST invoice generation response/error mapping).
- **docs/ARCHITECTURE.md** updated to reflect same-meter billing logic, `List<InvoiceResponse>` contract, and test coverage.

## 2026-06-24 — Tariff plans implemented

- Added versioned tariff plan domain model with `RESIDENTIAL`, `COMMERCIAL`, and `INDUSTRIAL` tariff types, effective windows, active state, and ordered slabs.
- Added tariff plan REST APIs for create, list, get by id, and deactivate/close version.
- Replaced flat-rate invoice tariff calculation with active tariff lookup by meter tariff type and invoice generated date.
- Added invoice tariff audit fields: tariff plan, tariff type, and tariff version.
- Added Flyway migrations for baseline billing schema plus default local/test tariff plans; Hibernate now validates schema.
- Added tariff service/controller integration tests and updated invoice tests for active tariff calculation.
- Validation: Gradle `test` passes.

## 2026-06-24 — P1 customer retention and invoice lifecycle

- **Safe customer retention**: `CustomerService.delete` now transitions customers to `INACTIVE` instead of physically deleting rows, preserving related meters, invoices, and entity audit history.
- **Customer API**: `DELETE /api/customers/{id}` remains available and returns `204 No Content`, with OpenAPI summary updated to "Deactivate a customer".
- **Invoice lifecycle APIs**: added `POST /api/invoices/{id}/pay` and `POST /api/invoices/{id}/cancel`, returning updated `InvoiceResponse`.
- **Invoice lifecycle rules**: `GENERATED` and `OVERDUE` may transition to `PAID` or `CANCELLED`; `PAID` and `CANCELLED` are terminal for pay/cancel APIs and invalid transitions return 422 via `BillingException`.
- **Tests added**: `CustomerRetentionIntegrationTest` for invoice-preserving customer deactivation and `InvoiceLifecycleIntegrationTest` for service/API lifecycle transitions.
- **Validation**: Gradle `test --rerun-tasks` passes.

## 2026-06-24 — P2 reliability hardening

- Added JPA auditing activation and optimistic locking via `opt_lock` version fields plus Flyway `V3__add_optimistic_locking.sql`.
- Added HTTP 409 handling for data integrity violations and optimistic locking conflicts using the shared `ApiError` shape.
- Added structured SLF4J lifecycle/conflict/error logging in services and global exception handling.
- Converted customer, meter, reading, invoice, and tariff list APIs to pageable responses with default page size 20 and `id ASC` ordering.
- Added customer-scoped invoice and meter APIs plus meter detail API; invoice detail API remains available.
- Added reliability integration coverage for auditing, optimistic locking, conflict handlers, pageable responses, customer-scoped APIs, meter detail, and invoice detail.
- Validation: Gradle `test --no-daemon` passes.

## 2026-06-24 — AQA integration and E2E coverage expansion

- Added full REST billing workflow coverage: customer creation, meter registration, two reading captures, invoice generation, and invoice payment through MockMvc API calls.
- Expanded invoice lifecycle matrix coverage for generated cancel, overdue pay, terminal pay rejection, cancelled pay rejection, and unknown invoice 404 responses.
- Expanded reliability coverage for audit timestamp mutation semantics, `opt_lock` version increments, meter/reading uniqueness constraints, and customer pagination ordering/boundary metadata.
- Expanded customer retention coverage to verify inactive customers remain readable/listed, meters remain queryable, and repeated delete remains `204 No Content`.
- Added current-behavior status guard coverage documenting that invoice generation is currently permissive for inactive customers and non-active meters.
- Validation: Gradle `test --quiet` passes with 54 tests.

## 2026-06-24 — V4 reference and demo data seed migration

- Added Flyway `V4__seed_reference_and_demo_data.sql` with fictional reference/demo seed data for invoice-generation demos.
- Reused V2 tariff plans as the single seeded tariff source to avoid duplicate-looking tariff rows.
- Added seven fictional active customers, one active meter per customer, and chronological increasing readings for each meter.
- Seeded four generated demo invoices from historical reading pairs while leaving latest reading pairs available for invoice-generation demos.
- Validation: Gradle `test` passes with 56 tests.

## 2026-06-24 — Pageable GET APIs tolerate Swagger placeholder sort

- Added shared `PageableSanitizer` to replace Swagger/OpenAPI placeholder sort values (`string` / `["string"]`) with the default `id ASC` sort before repository access.
- Applied sanitizer to customer, meter, reading, invoice, tariff, and customer-scoped pageable read services.
- Added integration coverage for pageable GET endpoints with `sort=["string"]`.
- Validation: affected reliability integration test and full Gradle `test` pass with 55 tests.

## 2026-06-24 — Flyway bootstrap integration tests

- Added `FlywayBootstrapIntegrationTest` with an isolated H2 database and Hibernate `ddl-auto=validate` to verify schema comes from Flyway migrations.
- Covered application context startup, four applied Flyway migrations, no pending migrations, no Flyway validation errors, seeded customers, meters, tariffs, readings, and demo invoices.
- Added repository assertions against seeded customer, meter reading, and tariff data, plus transactional invoice generation using seeded `CUST-R001` data.
- Saved final full-suite results in `TEST-RESULTS.md`.
- Validation: Gradle `test --no-daemon` passes with 74 tests.

## 2026-06-24 — Postman billing-service collection

- Added root Postman collection and local environment for billing-service: `EcoVolt-Billing.postman_collection.json` and `EcoVolt-Billing.postman_environment.json`.
- Covered customer, meter, reading, invoice, tariff detail/deactivation, customer-scoped meter/invoice, pagination, lifecycle, validation, missing entity, duplicate meter, duplicate-customer feasibility, and optimistic-lock feasibility scenarios.
- Added end-to-end workflow from customer creation through paid invoice verification and embedded Newman CI command.

## 2026-06-26 — Postman/Newman structured test suite

- Added `postman/` directory with full Newman-runnable API test suite.
- `postman/collections/EcoVolt-Billing.postman_collection.json`: 8 folders, 54 requests covering all Customer/Tariff/Meter/Reading/Invoice endpoints; E2E lifecycle folder (9 steps, Customer→active RESIDENTIAL tariff verification→Meter→Reading×2→Invoice→Pay→Verify PAID); Negative Tests folder covers validation errors, missing entities, duplicate meter/reading/tariff, invoice replay, invalid lifecycle transitions, and the current duplicate-customer contract gap.
- `postman/environments/local.postman_environment.json` and `ci.postman_environment.json`: no secrets, `baseUrl` defaulting to `http://localhost:8080`.
- `postman/scripts/run-newman.sh`: POSIX/Bash, `set -euo pipefail`, HTML+JUnit reports to `newman-reports/`, supports `BILLING_BASE_URL` env var override, uses global Newman or `npx` fallback, fails build on Newman non-zero exit.
- `.github/workflows/postman-newman.yml`: Java 25 (Temurin), Gradle `bootJar`, background service start, health poll (60 s, 2 s intervals), Node 20 + Newman, report upload as artifacts, JUnit report via `dorny/test-reporter`.
- `postman/README.md`: import guide, local Newman usage, CI/CD explanation, environment variable table.
- No secrets in any file; no teardown DELETEs for meter/reading/invoice; unique timestamp-based test data seeded via pre-request scripts.
- Validation: isolated local service on port 18080 passed Newman via `postman/scripts/run-newman.sh local` with 54 requests and 106 assertions.

## 2026-07-08 — RFC 10008 QUERY support for invoice pagination

- Added Spring MVC custom request-condition support for HTTP `QUERY` methods using `@QueryMethod`.
- Added `QUERY /api/invoices` as a safe/idempotent equivalent to pageable `GET /api/invoices`; it reuses `InvoiceService.findAll(Pageable)` and returns `Accept-Query: application/x-www-form-urlencoded`.
- Preserved existing `page`, `size`, and sanitized `sort=["string"]` pageable behavior.
- Added dedicated MockMvc regression coverage for `QUERY /api/invoices?page=0&size=1&sort=%5B%22string%22%5D`.

- Created foundational Rosetta documentation for context, architecture, TODOs, assumptions, requirements index/change tracking, agent memory, and reference-source policy.
- Created root and service README files for workspace navigation and local service entry points.
- Updated initialization workflow state to COMPLETE after Phase 8 verification.
