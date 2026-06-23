# PATTERNS CHANGES

## [2026-06-23] Phase 5 — Initial patterns inventory created

### Created

- `spring-domain-layering.md` — vertical slice structure, 4 domain occurrences
- `rest-controller-pattern.md` — REST controller conventions, 4 controller occurrences
- `jpa-entity-pattern.md` — JPA entity conventions, 4 entity occurrences
- `dto-mapping-pattern.md` — request/response record conventions, 7 DTO occurrences
- `exception-handling-pattern.md` — exception hierarchy + global handler, 5 service usages
- `service-or-throw-pattern.md` — cross-domain getXxxOrThrow lookup, 2 service occurrences
- `unique-key-generation-pattern.md` — do-while UUID business key generation, 2 service occurrences
- `INDEX.md` — master index listing all 7 patterns + 3 explicit skip reasons
- `CHANGES.md` — this file

### Skipped (with reason)

- `testing-pattern.md` — insufficient evidence; only one smoke test (`BillingServiceApplicationTests.contextLoads`); pattern not yet established
- `config/OpenApiConfig.java` — single class, no recurrence
- `tariff/TariffService.java` — single strategy instance; recurrence not yet present

### Source scanned

`billing-service/src/main/java/com/ecovolt/billing` — 35 Java files across 8 packages
