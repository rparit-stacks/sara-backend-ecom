-- Add cancellation fields to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT,
ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;
