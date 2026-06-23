# JPA Entity Pattern

## Description

All persistent entities:
1. Extend `BaseEntity` to inherit `createdAt` / `updatedAt` audit fields (populated via JPA Auditing).
2. Carry a full Lombok annotation stack: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.
3. Declare `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` with `Long` PK.
4. Define the table name and all named unique-constraint names explicitly in `@Table`.
5. Map enum columns with `@Enumerated(EnumType.STRING)` (never ORDINAL).
6. Use `FetchType.LAZY` for all associations.
7. Collections are initialised as `new ArrayList<>()` using `@Builder.Default`.

`BaseEntity` itself is `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`:
- `createdAt` — `@CreatedDate @Column(updatable = false)`
- `updatedAt` — `@LastModifiedDate`

## When to use

Every new persistent domain object. Extend `BaseEntity`; do not duplicate audit fields.

## Template

```java
@Entity
@Table(name = "{table_name}",
        uniqueConstraints = @UniqueConstraint(name = "uk_{table}_{field}", columnNames = "{column}"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class {Domain} extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "{business_key}", nullable = false, unique = true, updatable = false)
    private String {businessKey};

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private {Domain}Status status;

    // Many-to-one example
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "{fk_column}", nullable = false)
    private {Parent} {parent};

    // One-to-many example (owner side)
    @Builder.Default
    @OneToMany(mappedBy = "{parent}", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    private List<{Child}> {children} = new ArrayList<>();
}
```

## Evidence

- `billing-service/src/main/java/com/ecovolt/billing/common/BaseEntity.java` (shared superclass)
- `billing-service/src/main/java/com/ecovolt/billing/customer/Customer.java` (OneToMany meters + invoices)
- `billing-service/src/main/java/com/ecovolt/billing/meter/Meter.java` (ManyToOne customer, OneToMany readings)
- `billing-service/src/main/java/com/ecovolt/billing/reading/MeterReading.java` (ManyToOne meter, BigDecimal value)
- `billing-service/src/main/java/com/ecovolt/billing/invoice/Invoice.java` (multiple ManyToOne, composite unique constraint)
