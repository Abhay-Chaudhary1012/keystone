CREATE TABLE parts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    part_number VARCHAR(100) NOT NULL UNIQUE,
    unit_cost NUMERIC(12,2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_parts_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT chk_parts_stock_quantity CHECK (stock_quantity >= 0)
);

CREATE TABLE work_order_parts (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    part_id BIGINT NOT NULL REFERENCES parts(id),
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(12,2) NOT NULL,
    total_cost NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_wop_quantity CHECK (quantity > 0),
    CONSTRAINT chk_wop_unit_cost CHECK (unit_cost >= 0),
    CONSTRAINT chk_wop_total_cost CHECK (total_cost >= 0)
);

CREATE INDEX idx_work_order_parts_work_order ON work_order_parts(work_order_id);
CREATE INDEX idx_work_order_parts_part ON work_order_parts(part_id);

CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    minutes INTEGER NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_time_entry_minutes CHECK (minutes > 0)
);

CREATE INDEX idx_time_entries_work_order ON time_entries(work_order_id);
CREATE INDEX idx_time_entries_user ON time_entries(user_id);
