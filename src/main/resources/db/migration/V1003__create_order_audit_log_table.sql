-- Create order_audit_log table
CREATE TABLE IF NOT EXISTS order_audit_log (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    change_type VARCHAR(50) NOT NULL, -- STATUS_UPDATE, PRICE_UPDATE, ITEM_UPDATE, ADDRESS_UPDATE, etc.
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_audit_log_order_id ON order_audit_log(order_id);
CREATE INDEX IF NOT EXISTS idx_order_audit_log_created_at ON order_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_order_audit_log_change_type ON order_audit_log(change_type);
