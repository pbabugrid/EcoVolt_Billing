#Implementation Notes
## Recent Changes
- Included `WeekendPricingStrategy` ad-hoc task update.
- Implemented JSON Export feature for Customer Bills containing:
  - Updates to `pom.xml` adding Jackson dependencies (`jackson-databind` & `jackson-datatype-jsr310`).
  - Added new utility class `JsonExporter.java` inside `com.ecovolt.app`.
  - Added step 6 to `MainApplication.java`'s runtime to output bills right after database sync.
- Implemented `JsonExporterTest.java` utilizing JUnit 5 `@TempDir` to simulate unit tests locally.
- Added Jackson annotations (`@JsonCreator`, `@JsonProperty`) to `CsvRow.java` and authored `CsvRowTest.java` to test native serialization for input data models.

## Current State
- Everything compiles and integrates properly logic wise. Project defaults output file to `bills_output.json`.
- Uses standard Java classes without heavy frameworks like Spring Batch.
- Uses straight JDBC via DAOs rather than JPA/Hibernate.