# REQUIREMENTS CHANGES

This document tracks changes to requirements artifacts.
Content: requirement-source additions, clarifications, and scope decisions only.
Style: chronological, concise, and evidence-based.

## Baseline — 2026-06-23 — No original requirements found

- No original requirements files were found during initialization.
- Initial product understanding was recovered from `billing-service/HELP.md`, `gain.json`, source code, and Rosetta discovery docs.
- Payments and reporting are named in metadata/docs, not evidenced in source implementation, and confirmed as future scope.

## 2026-06-23 — Phase 7 clarifications

- Java 25 is the intended target.
- Payments and reporting are future scope.
- Invoice generation target is same-meter readings.
- H2 in-memory persistence is local development/test only.
- Generated build/cache artifacts stay ignored; ZIP archives are local/reference artifacts unless explicitly promoted.
- SDLC placeholders in `gain.json` are intentionally unresolved until real tooling values are known.
