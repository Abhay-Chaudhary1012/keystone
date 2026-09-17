-- =============================================
-- V1: Create customers table
-- =============================================
-- WHY THIS TABLE EXISTS:
--   Requirement: "Dispatcher/Manager can create and edit customers"
--   Requirement: "A site belongs to a customer"
--   Requirement: "Customers must only see their own organization's data"
--
-- This represents a CUSTOMER ORGANIZATION (e.g., "Acme Corporation"),
-- NOT an individual person. Individual customer users are in the 'users' table
-- with role='CUSTOMER' and a FK pointing back here.
--
-- RELATIONSHIPS:
--   customers 1──* sites      (a customer has many facility sites)
--   customers 1──* users      (customer-role users belong to an org)
--   customers 1──* work_orders (work orders are raised for a customer)
--
-- CONSTRAINTS:
--   'code' is UNIQUE — human-readable identifier (e.g., CUST-001)
--   'name' is NOT NULL — every customer org must have a name
--   'active' defaults to TRUE — supports soft-delete pattern

CREATE TABLE customers (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    code        VARCHAR(50)     NOT NULL UNIQUE,
    email       VARCHAR(255),
    phone       VARCHAR(50),
    address     TEXT,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- INDEX RATIONALE:
-- idx_customers_code   : Lookups by code are frequent (human-readable ID)
-- idx_customers_name   : Search/filter by name (search APIs)
-- idx_customers_active : Filter active/inactive customers in lists
CREATE INDEX idx_customers_code   ON customers(code);
CREATE INDEX idx_customers_name   ON customers(name);
CREATE INDEX idx_customers_active ON customers(active);
