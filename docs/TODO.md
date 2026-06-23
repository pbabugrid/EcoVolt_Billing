# TODO

This document tracks future improvements, unresolved gaps, and follow-up work.
Content: actionable TODOs only, not a changelog. Completed implementation history belongs in `agents/IMPLEMENTATION.md`.
Style: each entry header includes priority, when, what, and where.

## P1 Future Align invoice generation with same-meter target in billing-service

- Target behavior: generate invoices from two readings belonging to the same meter.
- Current source appears to select the latest two readings across all meters for a customer.
- Update repository query/service logic and add focused tests in a future coding workflow.

## P2 Future Replace SDLC placeholders in gain.json

- `gain.json` contains placeholders for issue tracking, wiki, build management, UX, infrastructure, IaC, hosting, logging, security, E2E tests, performance tests, and code graph.
- Placeholders are intentionally unresolved until real tools are known.
- Replace placeholders with confirmed integrations or remove unused categories during future workspace maintenance.

## P2 Future Add behavior-focused automated tests

- Add tests for validation failures, reading monotonicity, duplicate readings, invoice generation preconditions, duplicate invoice prevention, and error responses.
- Current repository evidence shows only a Spring context smoke test.

## P3 Future Define non-local persistence expectations

- Runtime configuration currently uses H2 in-memory for local development and tests.
- Define production-like persistence before documenting deployment expectations.
