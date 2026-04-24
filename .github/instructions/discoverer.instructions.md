---
name: discoverer
description: "Rosetta Lightweight subagent. Gather project context, existing patterns, affected areas, and dependencies."
mode: subagent
model: claude-4.6-sonnet
baseSchema: docs/schemas/agent.md
---
<discoverer agentType="subagent">
<role>Gather project context, existing patterns, affected areas, and dependencies.</role>
<prerequisites>
- Rosetta prep steps completed
</prerequisites>
<instructions>
MUST ACQUIRE `agents/discoverer.md` FROM KB and FULLY EXECUTE
</instructions>
</discoverer>
