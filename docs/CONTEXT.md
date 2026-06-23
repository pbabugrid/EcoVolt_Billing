# CONTEXT

This document captures product and domain context for the workspace.
Content: business purpose, stakeholder-visible capabilities, domain vocabulary, and known scope.
Style: concise, grep-friendly, non-technical. Technical architecture belongs in `docs/ARCHITECTURE.md`.

## Product Purpose

- EcoVolt Billing is a utility billing platform for managing utility customers, their meters, usage readings, and generated invoices.
- The system supports operational billing staff who need to register customers, assign meters, capture meter readings, and produce invoice records from usage.
- The repository evidence currently supports a single service named `billing-service`.

## Domain Scope Evidenced By Source

- Customer management: create, list, retrieve, update, and delete utility billing account holders.
- Meter management: register meters and assign them to customers.
- Meter reading capture: record dated meter readings and prevent inconsistent reading sequences.
- Invoice generation: generate an invoice from same-meter readings and a flat tariff.
- Invoice retrieval and lifecycle: list invoices, retrieve invoice details, mark invoices paid, and cancel invoices.

## Domain Vocabulary

- Customer: utility billing account holder.
- Meter: utility consumption measurement device assigned to a customer.
- Meter Reading: dated usage value recorded for a meter.
- Invoice: billing document generated from consumption between two readings.
- Tariff: pricing rule used to convert consumption units into invoice amount.

## Future Scope

- External payment processing is future scope; invoice payment status transitions are handled in the invoice domain.
- Reporting is future scope; no reporting source package or API is present under `billing-service/src/main/java/com/ecovolt/billing`.
