-- =============================================
-- V4: Create sites and work_orders tables
-- =============================================
-- CONTEXT:
--   M2 Core Work Orders requires a work_orders table.
--   Work orders reference a site (physical facility location).
--   The brief states: "A site belongs to a customer."
--   The sites table was planned in V1 comments but not yet created.
--
-- This migration creates both tables in dependency order:
--   1. sites       (depends on: customers)
--   2. work_orders (depends on: customers, sites, users)

-- =============================================
-- 1. Sites table
-- =============================================
-- A Site is a physical facility location belonging to a customer organization.
-- Examples: "Downtown Office", "Warehouse B", "Main Campus Building 3"
--
-- RELATIONSHIPS:
--   sites *──1 customers    (a site belongs to one customer org)
--   sites 1──* work_orders  (work is performed at a site)

CREATE TABLE sites (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    address     TEXT            NOT NULL,
    customer_id BIGINT          NOT NULL REFERENCES customers(id),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sites_customer_id ON sites(customer_id);
CREATE INDEX idx_sites_active      ON sites(active);

-- =============================================
-- 2. Work Orders table
-- =============================================
-- The central entity of the KEYSTONE platform.
--
-- RELATIONSHIPS:
--   work_orders *──1 customers  (which customer org this WO belongs to)
--   work_orders *──1 sites      (where the work is performed — nullable)
--   work_orders *──1 users      (assigned_technician — nullable until dispatched)
--   work_orders *──1 users      (created_by — who created this WO)
--
-- DESIGN DECISIONS:
--   'code' is a unique human-readable identifier (e.g., WO-000001).
--       Generated server-side, never user-supplied.
--   'site_id' is NULLABLE — a dispatcher may create a WO before knowing the site.
--   'assigned_technician_id' is NULLABLE — set later when dispatcher assigns.
--   'status' and 'priority' use VARCHAR + CHECK constraints matching Java enums.
--   'description' is NULLABLE — may be filled in later.
--
-- IMMUTABILITY RULE:
--   COMPLETED and CANCELLED are terminal states.
--   The service layer (not the DB) enforces: no edits after terminal state.

CREATE TABLE work_orders (
    id                      BIGSERIAL       PRIMARY KEY,
    code                    VARCHAR(50)     NOT NULL UNIQUE,
    title                   VARCHAR(255)    NOT NULL,
    description             TEXT,
    status                  VARCHAR(20)     NOT NULL,
    priority                VARCHAR(20)     NOT NULL,
    customer_id             BIGINT          NOT NULL REFERENCES customers(id),
    site_id                 BIGINT          REFERENCES sites(id),
    assigned_technician_id  BIGINT          REFERENCES users(id),
    created_by_id           BIGINT          NOT NULL REFERENCES users(id),
    notes                   TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_work_order_status CHECK (
        status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_work_order_priority CHECK (
        priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    )
);

-- INDEX RATIONALE:
-- idx_wo_code          : Lookup by human-readable code (frequent)
-- idx_wo_status        : Filter/group by status (Kanban board, dashboards)
-- idx_wo_priority      : Filter by priority
-- idx_wo_customer_id   : Customer data isolation (ownership enforcement)
-- idx_wo_site_id       : Filter WOs by site
-- idx_wo_technician    : Technician's assigned job list
-- idx_wo_created_by    : Audit — who created which WOs
CREATE INDEX idx_wo_code          ON work_orders(code);
CREATE INDEX idx_wo_status        ON work_orders(status);
CREATE INDEX idx_wo_priority      ON work_orders(priority);
CREATE INDEX idx_wo_customer_id   ON work_orders(customer_id);
CREATE INDEX idx_wo_site_id       ON work_orders(site_id);
CREATE INDEX idx_wo_technician    ON work_orders(assigned_technician_id);
CREATE INDEX idx_wo_created_by    ON work_orders(created_by_id);
