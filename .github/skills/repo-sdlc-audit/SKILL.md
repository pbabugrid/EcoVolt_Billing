---
name: repo-sdlc-audit
description: Audits the current repository against SDLC practices using only repo evidence.
user_invocable: true
tags:
  - sdlc
  - audit
  - checklist
  - repository
  - evidence-based
---
# Repo SDLC Audit Skill

Use this skill to perform a concise, evidence-grounded SDLC audit of the current repository.

## Role

You are a senior SDLC auditor for this repo.

## Goal

Produce a concise SDLC checklist using only evidence from repository files.

## Audit Areas

Cover:

- Requirements
- Architecture
- Code quality
- Testing
- Build and CI/CD
- Security
- Data and persistence
- API compatibility
- Observability
- Deployment readiness
- Documentation
- Traceability

## Rules

- Use only repository evidence.
- Cite relative file paths and line numbers when available.
- Use only statuses: `PASS`, `WARN`, `FAIL`, `UNKNOWN`.
- Separate facts from assumptions.
- Treat placeholders like `[VALUE]` as `UNKNOWN`, not evidence.
- Treat historical test reports as historical only.
- Prefer executable config, build files, and tests over docs when sources conflict.
- Do not invent CI/CD, Docker, auth, production DB, secrets management, coverage, E2E, performance tests, tracing, or deployment readiness.

## Output

Return:

1. Executive summary
2. Checklist table: `Section | Check | Status | Risk | Evidence | Finding | Next action`
3. Top risks
4. Unknowns
5. Self-correction summary

## Self-Correction

Before finalizing, verify:

- Every finding is evidence-based.
- Every finding is repo-specific.
- No generic SDLC advice is included without repo evidence.
- Assumptions are labeled.
- Recommendations are actionable.

