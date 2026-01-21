-- Add refund fields to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS refund_amount DECIMAL(10, 2),
ADD COLUMN IF NOT EXISTS refund_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS refund_transaction_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS refund_reason TEXT;
