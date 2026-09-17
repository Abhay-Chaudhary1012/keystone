package com.keystone.keystone_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * JPA entity mapped to the 'notifications' table.
 *
 * <p>Stores in-app notifications for users. Created automatically when
 * system events occur (e.g., work order assigned to a technician).</p>
 *
 * <p><strong>SCOPE:</strong> This is a minimal notification foundation.
 * External delivery (email, SMS, push) is NOT implemented.
 * The notification is stored in the database and can be queried by the
 * frontend to show a notification badge/list.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user who receives this notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The work order related to this notification (nullable for future general notifications). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    /** Notification type for categorization (e.g., "ASSIGNMENT", "STATUS_CHANGE"). */
    @Column(nullable = false, length = 50)
    private String type;

    /** Human-readable notification message. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Whether the user has read/acknowledged this notification. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
