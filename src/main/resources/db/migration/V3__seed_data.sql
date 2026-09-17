-- =============================================
-- V3: Seed development data
-- =============================================
-- PURPOSE:
--   Provides initial test data for development and testing.
--   Creates one sample customer organization and four users (one per role).
--
-- PASSWORDS:
--   All users have the password: password123
--   Stored as BCrypt hash with cost factor 10 ($2a$10$...)
--
--   BCrypt format: $2a$<cost>$<22-char-salt><31-char-hash>
--   - $2a = BCrypt version
--   - $10 = cost factor (2^10 = 1024 rounds — higher = slower = more secure)
--   - The salt is embedded IN the hash (no separate salt column needed)
--
-- NOTE: In production, these seed users should be replaced or disabled.
--       Never use weak passwords like 'password123' in production.

-- Sample customer organization
INSERT INTO customers (name, code, email, phone, address)
VALUES (
    'Acme Corporation',
    'CUST-001',
    'info@acme.com',
    '555-0100',
    '123 Business Ave, Springfield, IL 62701'
);

-- Seed users — one per role
-- All passwords: password123
-- BCrypt hash: $2a$10$waRtpWqENfhw7cZgugZtkerM.DryR2QutZCq7mqwdx3OlTqHrGQBC
INSERT INTO users (username, email, password_hash, full_name, phone, role, customer_id)
VALUES
    -- DISPATCHER: Internal staff, no customer_id
    ('dispatcher1', 'dispatcher@keystone.com',
     '$2a$10$waRtpWqENfhw7cZgugZtkerM.DryR2QutZCq7mqwdx3OlTqHrGQBC',
     'Diana Prince', '555-0001', 'DISPATCHER', NULL),

    -- TECHNICIAN: Internal staff, no customer_id
    ('technician1', 'tech1@keystone.com',
     '$2a$10$waRtpWqENfhw7cZgugZtkerM.DryR2QutZCq7mqwdx3OlTqHrGQBC',
     'Tony Stark', '555-0002', 'TECHNICIAN', NULL),

    -- MANAGER: Internal staff, no customer_id
    ('manager1', 'manager@keystone.com',
     '$2a$10$waRtpWqENfhw7cZgugZtkerM.DryR2QutZCq7mqwdx3OlTqHrGQBC',
     'Maria Hill', '555-0003', 'MANAGER', NULL),

    -- CUSTOMER: Linked to Acme Corporation (customer_id = 1)
    -- This FK is how we enforce: "customer sees only their own org's data"
    ('customer1', 'customer@acme.com',
     '$2a$10$waRtpWqENfhw7cZgugZtkerM.DryR2QutZCq7mqwdx3OlTqHrGQBC',
     'Bruce Wayne', '555-0004', 'CUSTOMER', 1);
