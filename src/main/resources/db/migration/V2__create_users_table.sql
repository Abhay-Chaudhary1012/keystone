-- =============================================
-- V2: Create users table
-- =============================================
-- WHY THIS TABLE EXISTS:
--   Requirement: "Login endpoint", "JWT authentication", "BCrypt password hashing"
--   Requirement: "Four roles: DISPATCHER, TECHNICIAN, MANAGER, CUSTOMER"
--   Requirement: "Role-Based Access Control", "Server-side authorization"
--   Requirement: "A CUSTOMER should only access data belonging to their own organization"
--
-- DESIGN DECISIONS:
--   1. A single 'users' table for ALL roles (no separate technician table)
--      → A User with role='TECHNICIAN' IS the technician
--      → A User with role='CUSTOMER' is linked to a customer org via customer_id
--
--   2. 'customer_id' is NULLABLE:
--      → DISPATCHER, TECHNICIAN, MANAGER have customer_id = NULL (internal staff)
--      → CUSTOMER has customer_id = NOT NULL (links to their organization)
--      → This enables the ownership check: "which org does this user belong to?"
--
--   3. 'password_hash' stores BCrypt-encoded passwords (never plaintext)
--
--   4. 'role' is stored as VARCHAR with a CHECK constraint, matching the Java enum.
--      PostgreSQL enums exist but VARCHAR + CHECK is simpler to maintain with Flyway.
--
-- RELATIONSHIPS:
--   users *──1 customers   (CUSTOMER-role users belong to a customer org)
--   users 1──* work_orders (as assigned technician or creator)
--   users 1──* time_entries
--   users 1──* part_usages
--   users 1──* notifications

CREATE TABLE users (
    id            BIGSERIAL       PRIMARY KEY,
    username      VARCHAR(100)    NOT NULL UNIQUE,
    email         VARCHAR(255)    NOT NULL UNIQUE,
    password_hash VARCHAR(255)    NOT NULL,
    full_name     VARCHAR(255)    NOT NULL,
    phone         VARCHAR(50),
    role          VARCHAR(20)     NOT NULL,
    customer_id   BIGINT          REFERENCES customers(id),
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- CHECK constraint ensures only valid roles can be stored
    -- This is a DB-level safety net backing up the Java enum validation
    CONSTRAINT chk_user_role CHECK (role IN ('DISPATCHER', 'TECHNICIAN', 'MANAGER', 'CUSTOMER'))
);

-- INDEX RATIONALE:
-- idx_users_username    : Login lookup (find user by username)
-- idx_users_email       : Unique constraint already creates an index, but explicit for clarity
-- idx_users_role        : Filter users by role (e.g., "list all technicians")
-- idx_users_customer_id : Join/filter by customer org (ownership enforcement)
-- idx_users_active      : Filter active/inactive users
CREATE INDEX idx_users_username    ON users(username);
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_role        ON users(role);
CREATE INDEX idx_users_customer_id ON users(customer_id);
CREATE INDEX idx_users_active      ON users(active);
