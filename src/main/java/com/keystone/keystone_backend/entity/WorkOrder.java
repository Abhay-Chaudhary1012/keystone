package com.keystone.keystone_backend.entity;

import com.keystone.keystone_backend.enums.Priority;
import com.keystone.keystone_backend.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the 'work_orders' table.
 *
 * <p>The central entity of the KEYSTONE platform. Represents a field service
 * job — from creation through assignment, execution, and completion.</p>
 *
 * <p><strong>WHY THIS ENTITY EXISTS:</strong></p>
 * <ul>
 *   <li>Requirement: "Dispatcher creates/manages Work Order"</li>
 *   <li>Requirement: "Work Orders must support CRUD"</li>
 *   <li>Requirement: "Unique human-readable Work Order code"</li>
 *   <li>Requirement: "Editable while OPEN, immutable once COMPLETED or CANCELLED"</li>
 * </ul>
 *
 * <p><strong>RELATIONSHIPS:</strong></p>
 * <ul>
 *   <li>{@code work_orders *──1 customers} — which customer org this WO belongs to</li>
 *   <li>{@code work_orders *──1 sites} — where the work is performed (nullable)</li>
 *   <li>{@code work_orders *──1 users (assignedTechnician)} — who performs the work (nullable until dispatched)</li>
 *   <li>{@code work_orders *──1 users (createdBy)} — who created this WO (audit trail)</li>
 * </ul>
 *
 * <p><strong>LIFECYCLE:</strong></p>
 * <pre>
 *   OPEN → ASSIGNED → IN_PROGRESS → COMPLETED
 *                   ↕              ↗
 *               ON_HOLD ──────────┘
 *   Any non-terminal → CANCELLED
 * </pre>
 * <p>Status transitions are enforced by the service layer (not this entity).
 * COMPLETED and CANCELLED are terminal/immutable states.</p>
 *
 * <p><strong>CODE GENERATION:</strong></p>
 * <p>The {@code code} field (e.g., "WO-000001") is a unique human-readable
 * identifier generated server-side. It is never user-supplied.</p>
 */
@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique human-readable code (e.g., "WO-000001").
     * Generated server-side, never user-supplied.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle state. Transitions are enforced by the service layer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    /**
     * The customer organization this work order belongs to.
     * Required — every WO must be for a specific customer.
     * Used for data isolation: customers only see their own WOs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * The physical site where the work is performed.
     * Nullable — may be set after initial creation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    /**
     * The technician assigned to perform this work.
     * Nullable — set by the dispatcher during assignment (M3 dispatch logic).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    /**
     * The user who created this work order.
     * Required — audit trail for who originated the request.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
