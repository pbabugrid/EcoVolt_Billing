# CODEMAP

## / — workspace root

Files:
- `.gitignore`
- `README.md`
- `billing-service.zip` _(archive; repository artifact)_
- `core-copilot-standalone-2.0.51-2026-06-23.zip` _(archive; Rosetta plugin package)_
- `gain.json`

### /.github — Rosetta plugin configuration

#### /.github/agents — 10 agent definitions

Files: `architect.agent.md`, `discoverer.agent.md`, `engineer.agent.md`, `executor.agent.md`, `planner.agent.md`, `prompt-engineer.agent.md`, `requirements-engineer.agent.md`, `researcher.agent.md`, `reviewer.agent.md`, `validator.agent.md`

#### /.github/configure — 8 IDE/tool configs

Files: `antigravity.md`, `claude-code.md`, `codex.md`, `cursor.md`, `github-copilot.md`, `jetbrains-junie.md`, `opencode.md`, `windsurf.md`

#### /.github/hooks — 1 file

Files: `hooks.json`

#### /.github/instructions — 6 bootstrap instruction files

Files: `bootstrap-core-policy.instructions.md`, `bootstrap-execution-policy.instructions.md`, `bootstrap-guardrails.instructions.md`, `bootstrap-hitl-questioning.instructions.md`, `bootstrap-rosetta-files.instructions.md`, `plugin-files-mode.instructions.md`

#### /.github/prompts — 32 workflow prompt files

Files: `INDEX.md`, `adhoc-flow.prompt.md`, `aqa-flow*.prompt.md` (9), `code-analysis-flow.prompt.md`, `coding-agents-prompting-flow.prompt.md`, `coding-flow.prompt.md`, `external-lib-flow.prompt.md`, `init-workspace-flow*.prompt.md` (9), `modernization-flow*.prompt.md` (9), `requirements-authoring-flow.prompt.md`, `research-flow.prompt.md`, `self-help-flow.prompt.md`, `testgen-flow*.prompt.md` (8)

#### /.github/rules — 6 rule files

Files: `INDEX.md`, `coding-iac-best-practices.md`, `prompt-best-practices.md`, `requirements-best-practices.md`, `requirements-use-best-practices.md`, `speckit-integration-policy.md`

#### /.github/skills — 30+ skill directories

Notable skills: `coding`, `dangerous-actions`, `debugging`, `deviation`, `hitl`, `init-workspace-*` (7), `load-context`, `natural-writing`, `orchestrator-contract`, `plan-manager`, `planning`, `questioning`, `reasoning`, `requirements-authoring`, `requirements-use`, `research`, `reverse-engineering`, `risk-assessment`, `self-learning`, `self-organization`, `sensitive-data`, `subagent-contract`, `tech-specs`, `testing`

### /agents — Rosetta agent state and memory

Files: `IMPLEMENTATION.md`, `MEMORY.md`, `init-workspace-flow-state.md`

### /docs — Rosetta documentation

Files: `ARCHITECTURE.md`, `ASSUMPTIONS.md`, `CODEMAP.md`, `CONTEXT.md`, `DEPENDENCIES.md`, `TECHSTACK.md`, `TODO.md`

Subdirectories: `PATTERNS/`, `REQUIREMENTS/`

### /plans — Rosetta plans (empty)

### /refsrc — reference source policy

Files: `INDEX.md`

---

## /billing-service — Spring Boot application root

Files:
- `.gitattributes`
- `.gitignore`
- `build.gradle`
- `gradlew`
- `gradlew.bat`
- `settings.gradle`
- `HELP.md`
- `README.md`

Generated / ignored: `.gradle/`, `build/`

### /billing-service/gradle/wrapper — Gradle wrapper

Files: `gradle-wrapper.jar` _(binary)_, `gradle-wrapper.properties`

### /billing-service/src/main/java/com/ecovolt/billing — 45 Java source files

Files (root): `BillingServiceApplication.java`

#### …/common — 1 file — shared base entity

Files: `BaseEntity.java`

#### …/config — 1 file — OpenAPI configuration

Files: `OpenApiConfig.java`

#### …/customer — 6 files — customer domain

Files: `Customer.java`, `CustomerController.java`, `CustomerRepository.java`, `CustomerService.java`, `CustomerStatus.java`

##### …/customer/dto — 2 files

Files: `CustomerRequest.java`, `CustomerResponse.java`

#### …/exception — 4 files — error handling

Files: `ApiError.java`, `BillingException.java`, `GlobalExceptionHandler.java`, `ResourceNotFoundException.java`

#### …/invoice — 7 files — invoice domain

Files: `Invoice.java`, `InvoiceController.java`, `InvoiceGenerationService.java`, `InvoiceRepository.java`, `InvoiceService.java`, `InvoiceStatus.java`

##### …/invoice/dto — 1 file

Files: `InvoiceResponse.java`

#### …/meter — 6 files — meter domain

Files: `Meter.java`, `MeterController.java`, `MeterRepository.java`, `MeterService.java`, `MeterStatus.java`

##### …/meter/dto — 2 files

Files: `MeterRequest.java`, `MeterResponse.java`

#### …/reading — 5 files — meter reading domain

Files: `MeterReading.java`, `MeterReadingController.java`, `MeterReadingRepository.java`, `MeterReadingService.java`

##### …/reading/dto — 2 files

Files: `MeterReadingRequest.java`, `MeterReadingResponse.java`

#### …/tariff — 8 files — tariff plans and calculation

Files: `TariffCalculation.java`, `TariffController.java`, `TariffPlan.java`, `TariffPlanRepository.java`, `TariffService.java`, `TariffSlab.java`, `TariffType.java`

##### …/tariff/dto — 4 files

Files: `TariffPlanRequest.java`, `TariffPlanResponse.java`, `TariffSlabRequest.java`, `TariffSlabResponse.java`

### /billing-service/src/main/resources — 3 files — runtime config and migrations

Files: `application.yaml`

Config categories: `spring.application`, `spring.datasource` (H2 in-memory), `spring.h2.console`, `spring.jpa` (ddl-auto, show-sql)

#### /billing-service/src/main/resources/db/migration — 2 files — Flyway migrations

Files: `V1__baseline_billing_schema.sql`, `V2__seed_default_tariff_plans.sql`

### /billing-service/src/test/java/com/ecovolt/billing — 6 files — automated tests

Files: `BillingServiceApplicationTests.java`

#### …/invoice — 2 files

Files: `InvoiceGenerationIntegrationTest.java`, `InvoiceGenerationServiceTest.java`

#### …/reading — 1 file

Files: `MeterReadingServiceTest.java`

#### …/tariff — 2 files

Files: `TariffControllerIntegrationTest.java`, `TariffServiceIntegrationTest.java`
