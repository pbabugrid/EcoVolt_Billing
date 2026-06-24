# REST Controller Pattern

## Description

All REST controllers share a consistent declaration style:
- `@RestController` + `@RequestMapping("/api/{resource}")` at class level
- `@RequiredArgsConstructor` for constructor injection of the service layer
- `@Tag(name, description)` (springdoc) at class level for OpenAPI grouping
- `@Operation(summary)` on every handler method
- POST handlers return `ResponseEntity<T>` with `HttpStatus.CREATED`
- Detail GET handlers return `T` directly; list GET handlers return `Page<T>` with `Pageable`
- Pageable endpoints use `@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)` unless a different domain order is required
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
    @Operation(summary = "List {domain}s with pagination")
    public Page<{Domain}Response> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return {domain}Service.findAll(pageable);
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

- `billing-service/src/main/java/com/ecovolt/billing/customer/CustomerController.java`
- `billing-service/src/main/java/com/ecovolt/billing/meter/MeterController.java`
- `billing-service/src/main/java/com/ecovolt/billing/reading/MeterReadingController.java`
- `billing-service/src/main/java/com/ecovolt/billing/invoice/InvoiceController.java`
- `billing-service/src/main/java/com/ecovolt/billing/tariff/TariffController.java`
