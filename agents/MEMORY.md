# AGENT MEMORY

Generalized reusable lessons from agent sessions.
Root causes converted into preventive rules, not incident-specific notes.
Entries are h3 headers with [ACTIVE|RETIRED] status.
Content: brief, grep-friendly, MECE across sections. 
Style: one-liner per entry, optional sub-bullets for context.
Keep template entries so that AI knows how to fill them in later on.

## Preventive Rules

### Verify source before documenting implementation [ACTIVE]
Use source files and existing Rosetta inventory as evidence; record conflicts in `docs/ASSUMPTIONS.md` instead of guessing.

### Keep sensitive configuration values out of generated docs [ACTIVE]
Describe configuration categories and behavior only; never copy raw secret-like or environment-specific values from runtime config.

### Keep product context separate from technical architecture [ACTIVE]
Put stakeholder-visible purpose in `docs/CONTEXT.md`; put modules, APIs, persistence, and build/runtime details in `docs/ARCHITECTURE.md`.

### Update tests when service contracts change [ACTIVE]
When replacing a simple method contract with richer domain output, update direct mocks and helper factories in the same pass.

### Align test fixtures with database uniqueness contracts [ACTIVE]
When adding integration tests, vary all columns covered by unique constraints unless the test explicitly targets that constraint.

### Assert actual centralized error messages after one observed run [ACTIVE]
For exception-handler coverage, confirm exact API error text from the real response before hard-coding assertions.

### Preserve every noun in seed-data requests [ACTIVE]
When implementing data migrations, map each requested data category to concrete rows or explicitly get approval before relying on earlier migrations.

### Validate demo data from user-visible tables [ACTIVE]
For seed migrations, assert the exact user-facing table outcomes, not only that migrations apply and APIs can generate data later.

### Reproduce generated-client placeholders for pageable APIs [ACTIVE]
When pageable APIs are documented through Swagger/OpenAPI, test generated placeholder query values in addition to hand-written valid parameters.

### Ground seed cardinality in migrations before approval [ACTIVE]
When asserting seeded row counts, count migration inserts directly before proposing exact cardinalities; if discovery conflicts with source, stop for explicit approval.

### <Generalized Preventive Rule> [ACTIVE|RETIRED]
[Root cause, Reasons, Problems]

## What Worked

### Pattern inventory accelerated architecture documentation [ACTIVE]
Use `docs/PATTERNS/INDEX.md` and source spot-checks together to avoid re-reading the whole codebase.

### <Generalized What Worked> [ACTIVE|RETIRED]
[Root cause, Reasons, Problems]

## What Failed

### Metadata can overstate implemented scope [ACTIVE]
Cross-check README/HELP/gain descriptions against source packages before claiming a capability exists.

### <Generalized What Failed> [ACTIVE|RETIRED]
[Hypothesis, Root cause, Reasons, Problems]

## Discoveries

### Build docs can disagree with executable build files [ACTIVE]
When language or runtime versions conflict, treat executable build configuration as effective state and record intent as unresolved.

### <Generalized Discovery> [ACTIVE|RETIRED]
[Usage, Reasons, Problems]
