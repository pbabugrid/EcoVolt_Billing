# REST Controller Pattern

## Description

All REST controllers share a consistent declaration style:
- `@RestController` + `@RequestMapping("/api/{resource}")` at class level
- `@RequiredArgsConstructor` for constructor injection of the service layer
- `@Tag(name, description)` (springdoc) at class level for OpenAPI grouping
- `@Operation(summary)` on every handler method
- POST handlers return `ResponseEntity<T>` with `HttpStatus.CREATED`
- GET handlers return `T` or `List<T>` directly (Spring auto-wraps with 200)
- `@Valid @RequestBody` used on all mutating endpoints

## When to use

Every new domain controller. Do not bypass this pattern by returning raw domain
entities or omitting OpenAPI annotations.

## Template

```java
@RestController
@RequestMapping("/api/{resources}")
@RequiredArgsConstructor
@Tag(name = "{Resources}", description = "{Human-readable description}")
public class {Domain}Controller {

    private final {Domain}Service {domain}Service;

    @PostMapping
    @Operation(summary = "Create a {domain}")
    public ResponseEntity<{Domain}Response> create(@Valid @RequestBody {Domain}Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body({domain}Service.create(request));
    }

    @GetMapping
    @Operation(summary = "List all {domain}s")
    public List<{Domain}Response> findAll() {
        return {domain}Service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a {domain} by id")
    public {Domain}Response findById(@PathVariable Long id) {
        return {domain}Service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a {domain}")
    public {Domain}Response update(@PathVariable Long id,
                                    @Valid @RequestBody {Domain}Request request) {
        return {domain}Service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a {domain}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        {domain}Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/customer/CustomerController.java` (full CRUD, lines 23-61)
- `billing-service/src/main/java/com/ecovolt/billing/meter/MeterController.java` (POST + GET, lines 19-38)
- `billing-service/src/main/java/com/ecovolt/billing/reading/MeterReadingController.java` (POST + GET, lines 20-39)
- `billing-service/src/main/java/com/ecovolt/billing/invoice/InvoiceController.java` (custom POST + two GETs, lines 17-44)
