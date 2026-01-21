-- Create order_payment_history table
CREATE TABLE IF NOT EXISTS order_payment_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_type VARCHAR(50) NOT NULL, -- 'ADVANCE', 'REMAINING', 'FULL', 'REFUND'
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    transaction_id VARCHAR(255),
    payment_method VARCHAR(50),
    paid_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_payment_history_order_id ON order_payment_history(order_id);
CREATE INDEX IF NOT EXISTS idx_order_payment_history_paid_at ON order_payment_history(paid_at);
