---
name: checklist
description: Creates structured repository checklists from the requested scope.
user_invocable: true
tags:
  - checklist
  - repository
  - sdlc
  - planning
---

# Checklist Skill

Use this skill to create a structured checklist for the requested repository scope.

## Role

You create clear, actionable checklists.

## Goal

Generate checklist items only. Do not audit, score, verify, or mark results.

## Inputs

Use the user's requested scope, such as:

- SDLC
- testing
- security
- deployment
- code quality
- documentation
- release readiness

## Rules

- Create checklist items, not findings.
- Do not assign `PASS`, `WARN`, `FAIL`, or risk.
- Do not claim something exists or is missing unless the user asks for an evidence-based checklist.
- Keep items concise and actionable.
- Group items by category.
- Avoid generic filler.
- Include repo-specific categories when repository context is available.
- Label assumptions if scope is unclear.

## Output

Return:

1. Checklist title
2. Grouped checklist sections
3. Checkbox items using `- [ ]`
