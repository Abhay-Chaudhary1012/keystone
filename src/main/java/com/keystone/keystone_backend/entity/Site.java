package com.keystone.keystone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the 'sites' table.
 *
 * <p>Represents a <strong>physical facility location</strong> belonging to a
 * customer organization. Examples: "Downtown Office", "Warehouse B".</p>
 *
 * <p><strong>WHY THIS ENTITY EXISTS:</strong></p>
 * <ul>
 *   <li>Requirement: "A site belongs to a customer"</li>
 *   <li>Work orders are performed at a specific site</li>
 *   <li>A customer can have multiple sites (one-to-many)</li>
 * </ul>
 *
 * <p><strong>RELATIONSHIPS:</strong></p>
 * <ul>
 *   <li>{@code sites *──1 customers} — every site belongs to one customer org</li>
 *   <li>{@code sites 1──* work_orders} — work orders reference the site where
 *       the job is performed</li>
 * </ul>
 */
@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    /**
     * The customer organization this site belongs to.
     * Every site must belong to exactly one customer — never null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
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
