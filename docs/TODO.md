# TODO

This document tracks future improvements, unresolved gaps, and follow-up work.
Content: actionable TODOs only, not a changelog. Completed implementation history belongs in `agents/IMPLEMENTATION.md`.
Style: each entry header includes priority, when, what, and where.

## P2 Future Replace SDLC placeholders in gain.json

- `gain.json` contains placeholders for issue tracking, wiki, build management, UX, infrastructure, IaC, hosting, logging, security, E2E tests, and performance tests.
- Placeholders are intentionally unresolved until real tools are known.
- Replace placeholders with confirmed integrations or remove unused categories during future workspace maintenance.

## P3 Future Define non-local persistence expectations

- Runtime configuration currently uses H2 in-memory for local development and tests.
- Define production-like persistence before documenting deployment expectations.
