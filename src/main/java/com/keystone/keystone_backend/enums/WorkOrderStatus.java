package com.keystone.keystone_backend.enums;

/**
 * Work Order lifecycle states as approved in Phase 1.
 *
 * <p><strong>STATE MACHINE:</strong></p>
 * <pre>
 *   OPEN → ASSIGNED → IN_PROGRESS → COMPLETED
 *                   ↕              ↗
 *               ON_HOLD ──────────┘
 *
 *   Any non-terminal state → CANCELLED
 * </pre>
 *
 * <p>COMPLETED and CANCELLED are <strong>terminal/immutable</strong> states.
 * Once a Work Order reaches either, it cannot be modified.</p>
 *
 * <p>Status transitions are NOT enforced by this enum — they will be
 * enforced by explicit service-layer lifecycle operations (assign, start,
 * hold, resume, complete, cancel) with server-side validation.</p>
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED
}
