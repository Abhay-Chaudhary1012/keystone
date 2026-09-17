package com.keystone.keystone_backend.entity;

import com.keystone.keystone_backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the 'users' table.
 *
 * <p>Represents <strong>any user</strong> in the system — dispatchers, technicians,
 * managers, AND customer portal users. The {@link #role} field determines what the
 * user is allowed to do (RBAC). There is intentionally NO separate "Technician" entity;
 * a User with role=TECHNICIAN is the technician.
 *
 * <p><strong>WHY THIS ENTITY EXISTS:</strong>
 * <ul>
 *   <li>Requirement: "Login endpoint", "JWT authentication", "BCrypt password hashing"</li>
 *   <li>Requirement: "Four roles: DISPATCHER, TECHNICIAN, MANAGER, CUSTOMER"</li>
 *   <li>Requirement: "Role-Based Access Control", "Server-side authorization"</li>
 * </ul>
 *
 * <h3>Key Design — Customer Ownership:</h3>
 * <p>The {@link #customer} field (FK to customers table) is <strong>nullable</strong>:</p>
 * <ul>
 *   <li>DISPATCHER, TECHNICIAN, MANAGER → customer_id is NULL (they are internal staff)</li>
 *   <li>CUSTOMER → customer_id is NOT NULL (links them to their organization)</li>
 * </ul>
 * <p>This is how we enforce the requirement: "A CUSTOMER should only access data
 * belonging to their own organization." When a customer-role user makes an API request,
 * we extract their customer_id from the JWT/session and use it to scope all queries.</p>
 *
 * <h3>Annotation Deep-Dive:</h3>
 * <ul>
 *   <li>{@code @ManyToOne(fetch = FetchType.LAZY)} — Many users can belong to one customer.
 *       LAZY means the Customer object is NOT loaded from the DB until you actually
 *       call {@code user.getCustomer()}. This prevents loading unnecessary data.
 *       <br><strong>INTERVIEW TIP:</strong> Default fetch for @ManyToOne is EAGER (loads
 *       immediately). We override to LAZY for performance. EAGER can cause N+1 query
 *       problems when loading a list of users.</li>
 *   <li>{@code @JoinColumn(name = "customer_id")} — Specifies which DB column holds
 *       the foreign key. Without this, Hibernate would generate a name like "customer_id"
 *       anyway, but being explicit makes the mapping clear.</li>
 *   <li>{@code @Enumerated(EnumType.STRING)} — Stores the role as the enum's NAME
 *       (e.g., "DISPATCHER") not its ordinal position (0, 1, 2...).
 *       <br><strong>INTERVIEW TIP:</strong> NEVER use EnumType.ORDINAL in production.
 *       If someone inserts a new enum value at position 0, every existing row's role
 *       silently changes meaning.</li>
 * </ul>
 *
 * <h3>Security Note:</h3>
 * <p>{@code passwordHash} stores a BCrypt-encoded password. We never store plaintext.
 * BCrypt includes the salt IN the hash string (the $2a$10$... prefix), so we don't
 * need a separate salt column.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt-encoded password hash.
     * Format: $2a$10$[22-char salt][31-char hash]
     * The "10" is the cost factor (2^10 = 1024 rounds of hashing).
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 50)
    private String phone;

    /**
     * User's role — determines RBAC permissions across all endpoints.
     * Stored as VARCHAR in the DB (e.g., "DISPATCHER"), not as an integer ordinal.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * The customer organization this user belongs to.
     * NULL for internal staff (DISPATCHER, TECHNICIAN, MANAGER).
     * NOT NULL for CUSTOMER-role users (links them to their org).
     *
     * <p>This is the key to ownership enforcement: when a CUSTOMER user hits an API,
     * we read this field to scope their data access to their own organization only.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
