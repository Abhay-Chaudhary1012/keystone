package com.keystone.keystone_backend.enums;

/**
 * Work Order priority levels.
 *
 * <p>The brief requires priority on work orders but does NOT specify
 * exact SLA durations per priority level. SLA duration mapping will
 * be implemented in a later milestone when specified.</p>
 *
 * <p>Stored as VARCHAR in the DB via {@code @Enumerated(EnumType.STRING)}
 * with a CHECK constraint as a safety net.</p>
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
