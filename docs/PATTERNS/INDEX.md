# PATTERNS INDEX

Patterns extracted from `billing-service/src/main/java/com/ecovolt/billing` (35 Java source files, Spring Boot 3.5 / Java 25).

---

## spring-domain-layering — Vertical slice: Entity/Repository/Service/Controller/dto

Every domain module (customer, meter, reading, invoice) repeats an identical four-layer
vertical slice. Adding a new domain means replicating this file set. Evidence: 4 domains.

File: `docs/PATTERNS/spring-domain-layering.md`

## rest-controller-pattern — @RestController + OpenAPI annotations + CRUD conventions

All REST controllers declare `@RestController`, `@RequestMapping("/api/{resource}")`,
`@RequiredArgsConstructor`, `@Tag`, per-method `@Operation`. POST returns 201 via
`ResponseEntity`; GETs return the DTO directly. Evidence: 4 controllers.

File: `docs/PATTERNS/rest-controller-pattern.md`

## jpa-entity-pattern — @Entity extending BaseEntity with Lombok builder + audit fields

All JPA entities extend the shared `BaseEntity` (`createdAt`/`updatedAt`), carry the
full Lombok builder stack, use `GenerationType.IDENTITY` PK, `EnumType.STRING`, named
unique constraints, and `FetchType.LAZY` associations. Evidence: 4 entities.

File: `docs/PATTERNS/jpa-entity-pattern.md`

## dto-mapping-pattern — Java records: Request (Bean Validation) + Response (static from())

Inbound DTOs are Java `record` types with per-field Bean Validation constraints.
Outbound DTOs are Java `record` types with a single static `from(Entity)` factory.
Evidence: 7 DTO classes across 4 domains.

File: `docs/PATTERNS/dto-mapping-pattern.md`

## exception-handling-pattern — Typed exceptions + @RestControllerAdvice + ApiError record

`ResourceNotFoundException` (→ 404) and `BillingException` (→ 422) are thrown from
services. `GlobalExceptionHandler` centralises mapping to a uniform `ApiError` record.
Evidence: 4 exception classes, used in 5 services.

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

### tariff/TariffService.java

Single-instance strategy service; pattern coverage is pending if a second tariff strategy is introduced.

### BillingServiceApplicationTests.java

Single smoke test (`contextLoads`); no recurring test pattern to document.
