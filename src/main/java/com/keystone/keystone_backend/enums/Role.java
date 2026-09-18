package com.keystone.keystone_backend.enums;

/**
 * Defines the four application roles as specified in the KEYSTONE requirements.
 *
 * <p>WHY AN ENUM?
 * <ul>
 *   <li>Type safety — the compiler prevents typos like "DISPATCHR"</li>
 *   <li>Stored as VARCHAR in the DB via {@code @Enumerated(EnumType.STRING)}</li>
 *   <li>The DB also has a CHECK constraint as a safety net</li>
 * </ul>
 *
 * <p>INTERVIEW TIP: Always use {@code EnumType.STRING}, never {@code EnumType.ORDINAL}.
 * Ordinal stores the enum's position (0, 1, 2...) — if you reorder the enum, all
 * existing database rows silently get the wrong role. STRING stores the actual name.
 *
 * <p>ROLE DESCRIPTIONS:
 * <ul>
 *   <li>DISPATCHER — Creates/manages work orders, assigns technicians</li>
 *   <li>TECHNICIAN — Performs field service jobs, logs parts and time</li>
 *   <li>MANAGER — Monitors operations, SLA compliance, dashboard access</li>
 *   <li>CUSTOMER — Raises service requests, tracks their own org's requests</li>
 * </ul>
 */
public enum Role {
    DISPATCHER,
    TECHNICIAN,
    MANAGER,
    CUSTOMER
}
