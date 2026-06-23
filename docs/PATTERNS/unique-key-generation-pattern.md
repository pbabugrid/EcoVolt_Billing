# Unique Business Key Generation Pattern

## Description

Human-readable, collision-resistant business keys (customer numbers, invoice numbers)
are generated inside the owning service using a `do-while` loop that retries until the
candidate does not already exist in the database. The candidate is constructed from a
domain prefix + UUID substring:

- Customer number: `CUST-` + 8-char uppercase UUID segment
- Invoice number: `INV-{year}-` + 8-char uppercase UUID segment

The retry loop guards against the (extremely rare) collision without exposing the
generation logic to the HTTP layer.

## When to use

When a domain object requires a unique, user-visible identifier that must be
collision-safe but is not the surrogate PK. Do NOT use for surrogate PKs
(use `GenerationType.IDENTITY` instead).

## Template

```java
private String generate{Domain}Number() {
    String candidate;
    do {
        candidate = "{PREFIX}-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    } while ({domain}Repository.existsBy{DomainNumber}(candidate));
    return candidate;
}
```

Repository must declare a derived query:

```java
boolean existsBy{DomainNumber}(String {domainNumber});
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/customer/CustomerService.java`
  — `generateCustomerNumber()` (lines 71-76); repo method in `CustomerRepository.java` (line 6)
- `billing-service/src/main/java/com/ecovolt/billing/invoice/InvoiceGenerationService.java`
  — `generateInvoiceNumber()` (lines 83-89); repo method in `InvoiceRepository.java` (line 7)
