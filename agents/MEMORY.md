# Agent Memory

Agent workspace initialized. Context loaded for EcoVolt_Billing.

## Lessons Learned & Preventive Rules

### 1. Serialization without Default Constructors (Jackson)
- **Root Cause**: Jackson's `ObjectMapper` cannot intrinsically de-serialize an object (like `CustomerBill`) unless there is a default (no-args) constructor.
- **Rule**: ALWAYS verify domain models possess a default constructor (or `@JsonCreator`) before authoring tests attempting `mapper.readValue(...)`. Do NOT blindly add default constructors to production files to fix test compilation issues; request HITL authorization first.

### 2. Blind Coding (Shallow Mapping Models)
- **Root Cause**: Hallucinating constructors/methods (e.g., `CustomerBill(int, LocalDate, LocalDate)`) by relying only on high-level `CODEMAP.md`.
- **Rule**: ALWAYS read the source `.java` file of the entity (`cat` or `read_file`) before writing logic against it. Never guess method signatures.

### 3. Broken State Tracking
- **Root Cause**: Missing the sequential workflow step to log activities in `IMPLEMENTATION.md` and failure learnings in `MEMORY.md`.
- **Rule**: At the end of every active tool-call chain that involves writing code, unconditionally review and update both file states.
