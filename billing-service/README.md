# EcoVolt Billing Service

Spring Boot service for utility billing domain operations.

## Capabilities

- Customer management
- Meter registration and listing
- Meter reading capture
- Invoice generation and retrieval
- Flat tariff calculation

Payments and reporting are future scope and are not implemented in source packages yet.

## Build

```sh
./gradlew build
```

## Test

```sh
./gradlew test
```

## Run

```sh
./gradlew bootRun
```

## Local URLs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console`

## Notes

- Intended Java target is 25 and the effective toolchain is defined in `build.gradle`.
- Workspace-level architecture and patterns are documented under `../docs/`.
