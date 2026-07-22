# PATTERNS INDEX

Patterns extracted from `billing-service/src/main/java/com/ecovolt/billing` (50 main Java source files, Spring Boot 3.5 / Java 25).

---

## spring-domain-layering — Vertical slice: Entity/Repository/Service/Controller/dto

Domain modules (customer, meter, reading, invoice, tariff) repeat the vertical-slice
structure. Adding a new domain means replicating this file set. Evidence: 5 domains.

File: `docs/PATTERNS/spring-domain-layering.md`

## rest-controller-pattern — @RestController + OpenAPI annotations + CRUD conventions

All REST controllers declare `@RestController`, `@RequestMapping("/api/{resource}")`,
`@RequiredArgsConstructor`, `@Tag`, per-method `@Operation`. POST returns 201 via
`ResponseEntity`; GETs return the DTO directly. Evidence: 5 controllers.

File: `docs/PATTERNS/rest-controller-pattern.md`

## jpa-entity-pattern — @Entity extending BaseEntity with Lombok builder + audit fields

All JPA entities extend the shared `BaseEntity` (`createdAt`/`updatedAt`), carry the
full Lombok builder stack, use `GenerationType.IDENTITY` PK, `EnumType.STRING`, named
unique constraints, and `FetchType.LAZY` associations. Evidence: 6 entities.

File: `docs/PATTERNS/jpa-entity-pattern.md`

## dto-mapping-pattern — Java records: Request (Bean Validation) + Response (static from())

Inbound DTOs are Java `record` types with per-field Bean Validation constraints.
Outbound DTOs are Java `record` types with a single static `from(Entity)` factory.
Evidence: 12 DTO classes across 5 domains.

File: `docs/PATTERNS/dto-mapping-pattern.md`

## exception-handling-pattern — Typed exceptions + @RestControllerAdvice + ApiError record

`ResourceNotFoundException` (→ 404) and `BillingException` (→ 422) are thrown from
services. `GlobalExceptionHandler` centralises mapping to a uniform `ApiError` record.
Evidence: 4 exception classes, used across domain services.

File: `docs/PATTERNS/exception-handling-pattern.md`

## service-or-throw-pattern — Public getXxxOrThrow() for cross-domain entity resolution

Services expose a public `getXxxOrThrow(Long id)` that returns a managed entity or
throws `ResourceNotFoundException`. Cross-domain services call this instead of injecting
the foreign repository. Evidence: `CustomerService`, `MeterService`.

File: `docs/PATTERNS/service-or-throw-pattern.md`

## unique-key-generation-pattern — Do-while UUID-substring loop for human-readable business keys

Customer numbers (`CUST-{8-char}`) and invoice numbers (`INV-{year}-{8-char}`) are
generated in a retry loop that checks DB uniqueness before accepting the candidate.
Evidence: `CustomerService`, `InvoiceGenerationService`.

File: `docs/PATTERNS/unique-key-generation-pattern.md`

---

## Modules with explicit skip reasons

### config/OpenApiConfig.java

Single configuration class; no recurring structure to abstract.

### config/QueryMethodWebMvcConfig.java and common/http

Single custom HTTP method extension for RFC 10008 QUERY support; keep as architecture documentation, not a reusable pattern, until another custom method integration exists.

### common/PageableSanitizer.java

Single cross-cutting helper for Swagger/OpenAPI pageable placeholder tolerance; not a recurring structure.

### tariff/TariffService.java

Single tariff calculation service; controller, DTO, entity, and repository conventions are already covered by the main patterns.

### billing-service tests

Current tests include smoke, service, integration, workflow, Flyway, tariff, customer, invoice, reading, and reliability coverage. A reusable test pattern document remains pending until the test conventions stabilize across more modules.
