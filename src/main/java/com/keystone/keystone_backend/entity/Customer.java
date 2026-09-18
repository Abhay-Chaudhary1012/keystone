package com.keystone.keystone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the 'customers' table.
 *
 * <p>Represents a <strong>customer organization</strong> (e.g., "Acme Corporation"),
 * NOT an individual person. Individual customer portal users are {@link User} entities
 * with role=CUSTOMER and a foreign key pointing to this entity.
 *
 * <p><strong>WHY THIS ENTITY EXISTS:</strong>
 * <ul>
 *   <li>Requirement: "Dispatcher/Manager can create and edit customers"</li>
 *   <li>Requirement: "A site belongs to a customer"</li>
 *   <li>Requirement: "Customers must only see their own organization's data"</li>
 * </ul>
 *
 * <h3>Key Annotations Explained:</h3>
 * <ul>
 *   <li>{@code @Entity} — Marks this class as a JPA entity (Hibernate will manage it)</li>
 *   <li>{@code @Table(name = "customers")} — Maps to the 'customers' table.
 *       Without this, Hibernate would use the class name "Customer" as the table name,
 *       which could differ in case/pluralization.</li>
 *   <li>{@code @Id} — Marks the primary key field</li>
 *   <li>{@code @GeneratedValue(strategy = GenerationType.IDENTITY)} — Uses PostgreSQL's
 *       BIGSERIAL auto-increment. IDENTITY strategy tells Hibernate to let the DB
 *       generate the ID (vs. SEQUENCE which uses a separate DB sequence).</li>
 *   <li>{@code @Column} — Customizes column mapping (nullability, length, uniqueness)</li>
 *   <li>{@code @PrePersist} / {@code @PreUpdate} — JPA lifecycle callbacks that run
 *       automatically before INSERT / UPDATE operations</li>
 * </ul>
 *
 * <h3>Lombok Annotations:</h3>
 * <ul>
 *   <li>{@code @Getter} / {@code @Setter} — Auto-generates all getters and setters</li>
 *   <li>{@code @NoArgsConstructor} — Required by JPA (Hibernate needs a no-arg constructor)</li>
 *   <li>{@code @AllArgsConstructor} — Convenience constructor with all fields</li>
 *   <li>{@code @Builder} — Enables the builder pattern: {@code Customer.builder().name("Acme").build()}</li>
 * </ul>
 *
 * <h3>INTERVIEW TIP:</h3>
 * <p>"Why use DTOs instead of exposing this entity directly from REST APIs?"
 * <br>→ Entities are tied to Hibernate's session/proxy mechanism. Returning them from
 * controllers can cause LazyInitializationException, expose internal fields (like
 * created_at), and tightly couple your API contract to your DB schema.</p>
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Human-readable unique code (e.g., "CUST-001").
     * Used for display and lookup instead of exposing internal DB IDs.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    /**
     * Soft-delete flag. When false, the customer is deactivated but data is preserved.
     * Hard-deleting customers would cascade-destroy sites, work orders, and history.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback — runs automatically BEFORE the first INSERT.
     * Sets both timestamps so they're never null in the database.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback — runs automatically BEFORE every UPDATE.
     * Keeps updatedAt current without manual code in every service method.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
