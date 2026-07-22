# TECHSTACK

This document captures detected technology stack information for the workspace.
Content: languages, build tools, frameworks, persistence, documentation, code generation, testing, and project identity. Style: concise, evidence-grounded, and grep-friendly.

## Language

| Item | Value | Source |
|------|-------|--------|
| Java | 25 (toolchain) | `billing-service/build.gradle` |
| Java target | 25 | User-confirmed during Rosetta initialization |

## Build

| Item | Value | Source |
|------|-------|--------|
| Build tool | Gradle Wrapper | `billing-service/gradlew` |
| Gradle version | 8.14.5 | `billing-service/gradle/wrapper/gradle-wrapper.properties` |
| Toolchains resolver | foojay-resolver-convention 0.10.0 | `billing-service/settings.gradle` |
| Repository | mavenCentral() | `billing-service/build.gradle` |

## Framework

| Item | Value | Source |
|------|-------|--------|
| Spring Boot | 3.5.15 | `billing-service/build.gradle` plugin |
| Spring Dependency Management | 1.1.7 | `billing-service/build.gradle` plugin |
| Spring Web (MVC) | managed by Boot BOM | `billing-service/build.gradle` |
| Spring Data JPA | managed by Boot BOM | `billing-service/build.gradle` |
| Spring Boot Actuator | managed by Boot BOM | `billing-service/build.gradle` |
| Spring Boot Validation | managed by Boot BOM | `billing-service/build.gradle` |

## Persistence

| Item | Value | Source |
|------|-------|--------|
| ORM | Hibernate (via Spring Data JPA) | `build.gradle` + `application.yaml` `jpa.hibernate.ddl-auto: validate` |
| Migrations | Flyway SQL migrations | `billing-service/src/main/resources/db/migration` |
| Database | H2 in-memory (`jdbc:h2:mem:ecovolt`) | `billing-service/src/main/resources/application.yaml`; local development/test only |
| H2 Console | enabled | `application.yaml`; local development/test only |

## API Documentation

| Item | Value | Source |
|------|-------|--------|
| OpenAPI / Swagger UI | springdoc-openapi-starter-webmvc-ui 2.8.14 | `billing-service/build.gradle` |
| Swagger UI URL | `/swagger-ui.html` | `billing-service/HELP.md` |
| H2 Console URL | `/h2-console` | `billing-service/HELP.md` |

## Code Generation

| Item | Value | Source |
|------|-------|--------|
| Lombok | compileOnly + annotationProcessor (main + test) | `billing-service/build.gradle` |

## Testing

| Item | Value | Source |
|------|-------|--------|
| Spring Boot Test | managed by Boot BOM | `billing-service/build.gradle` |
| JUnit Platform Launcher | testRuntimeOnly | `billing-service/build.gradle` |
| Test runner | JUnit Platform (`useJUnitPlatform()`) | `billing-service/build.gradle` |

## Project Identity

| Item | Value |
|------|-------|
| Group | `com.ecovolt` |
| Artifact | `billing-service` |
| Version | `0.0.1-SNAPSHOT` |
| Root package | `com.ecovolt.billing` |
