# ASSUMPTIONS

This document records unknowns, contradictions, and facts requiring human confirmation.
Content: each entry states the assumption, confidence, evidence, and target file to update when resolved.
Style: concise, forward-referenced, and revalidated after documentation changes.

## Resolved Java target

- Resolution: Java 25 is the intended target.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; `billing-service/build.gradle`.
- Updated: `docs/TECHSTACK.md`, `docs/ARCHITECTURE.md`, `billing-service/HELP.md`.

## Resolved payment scope

- Resolution: Payment is future scope, not current implementation.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; source search under `billing-service/src/main/java/com/ecovolt/billing` found no payment package, API, or class.
- Updated: `docs/CONTEXT.md`, `docs/REQUIREMENTS/CHANGES.md`.

## Resolved reporting scope

- Resolution: Reporting is future scope, not current implementation.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; source search under `billing-service/src/main/java/com/ecovolt/billing` found no reporting package, API, or class.
- Updated: `docs/CONTEXT.md`, `docs/REQUIREMENTS/CHANGES.md`.

## SDLC metadata placeholders unresolved

- Assumption: Placeholder values in `gain.json` are intentionally unresolved initialization scaffolding until real SDLC integrations are known.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; multiple bracketed placeholder values remain in `gain.json`.
- Target when resolved: `gain.json`, `docs/CONTEXT.md`, `agents/MEMORY.md` if operational workflow rules change.

## Resolved generated artifacts policy

- Resolution: Generated build/cache artifacts stay ignored; ZIP archives are local/reference artifacts unless explicitly promoted.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization.
- Updated: `.gitignore`, `docs/CODEMAP.md`, `refsrc/INDEX.md`.

## Resolved invoice reading selection target

- Resolution: Target invoice generation should use same-meter readings.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; implemented by per-meter invoice generation using `MeterReadingRepository.findTop2ByMeter_IdOrderByReadingDateDescIdDesc`.
- Updated: `docs/CONTEXT.md`, `docs/ARCHITECTURE.md`, `docs/TODO.md`, `agents/IMPLEMENTATION.md`, and invoice generation tests.

## Resolved H2 persistence scope

- Resolution: H2 in-memory persistence is local development/test only.
- Confidence: High.
- Evidence: user confirmation during Rosetta initialization; `billing-service/src/main/resources/application.yaml`.
- Updated: `docs/TECHSTACK.md`, `docs/ARCHITECTURE.md`, `docs/TODO.md`.
