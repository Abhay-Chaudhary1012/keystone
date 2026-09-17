-- =============================================
-- V5: Create notifications table
-- =============================================
-- CONTEXT:
--   The project brief requires "technician is notified when a Work Order is assigned."
--   This migration creates a minimal internal notifications table to store
--   in-app notifications. External delivery (email/SMS) is NOT implemented.
--
-- DESIGN:
--   - Simple per-user notification record
--   - 'read' flag for UI badge counts
--   - 'type' for future categorization (ASSIGNMENT, STATUS_CHANGE, etc.)
--   - References the work order that triggered the notification

CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id),
    work_order_id   BIGINT          REFERENCES work_orders(id),
    type            VARCHAR(50)     NOT NULL,
    message         TEXT            NOT NULL,
    read            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id     ON notifications(user_id);
CREATE INDEX idx_notifications_user_read   ON notifications(user_id, read);
