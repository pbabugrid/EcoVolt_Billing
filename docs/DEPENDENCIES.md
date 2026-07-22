# DEPENDENCIES

This document captures direct project dependencies for the workspace.
Content: declared plugins and dependencies with versions or dependency-management source. Style: concise, evidence-grounded, and grep-friendly.

Source: `billing-service/build.gradle`

## Gradle Plugins

| Plugin | Version |
|--------|---------|
| `org.springframework.boot` | 3.5.15 |
| `io.spring.dependency-management` | 1.1.7 |
| `java` | (built-in) |
| `org.gradle.toolchains.foojay-resolver-convention` | 0.10.0 |

## Runtime / Implementation Dependencies

| Coordinate | Version | Notes |
|-----------|---------|-------|
| `org.springframework.boot:spring-boot-starter-actuator` | managed by Boot BOM | Health, metrics endpoints |
| `org.springframework.boot:spring-boot-starter-data-jpa` | managed by Boot BOM | JPA + Hibernate |
| `org.springframework.boot:spring-boot-starter-validation` | managed by Boot BOM | Bean Validation (Jakarta) |
| `org.springframework.boot:spring-boot-starter-web` | managed by Boot BOM | Spring MVC, embedded Tomcat |
| `org.flywaydb:flyway-core` | managed by Boot BOM | SQL schema migrations |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.8.14 | Swagger UI + OpenAPI 3 |

## Runtime-Only Dependencies

| Coordinate | Version | Notes |
|-----------|---------|-------|
| `com.h2database:h2` | managed by Boot BOM | In-memory H2 database |

## Compile-Only Dependencies

| Coordinate | Version | Notes |
|-----------|---------|-------|
| `org.projectlombok:lombok` | managed by Boot BOM | Annotation processor (main) |

## Annotation Processors

| Coordinate | Scope | Notes |
|-----------|-------|-------|
| `org.projectlombok:lombok` | `annotationProcessor` (main) | Generates boilerplate at compile time |
| `org.projectlombok:lombok` | `testAnnotationProcessor` | Lombok in test sources |

## Test Dependencies

| Coordinate | Version | Notes |
|-----------|---------|-------|
| `org.springframework.boot:spring-boot-starter-test` | managed by Boot BOM | JUnit 5, Mockito, AssertJ, Spring Test |
| `org.projectlombok:lombok` | managed by Boot BOM | `testCompileOnly` |
| `org.junit.platform:junit-platform-launcher` | managed by Boot BOM | `testRuntimeOnly` |

## Dependency Resolution

| Item | Value |
|------|-------|
| Repository | Maven Central |
| BOM source | Spring Boot 3.5.15 via `io.spring.dependency-management` |
