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

- Created foundational Rosetta documentation for context, architecture, TODOs, assumptions, requirements index/change tracking, agent memory, and reference-source policy.
- Created root and service README files for workspace navigation and local service entry points.
- Updated initialization workflow state to COMPLETE after Phase 8 verification.
