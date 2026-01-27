-- Per-item GST (rate % and amount) for admin ledger display
ALTER TABLE order_items
ADD COLUMN IF NOT EXISTS gst_rate DECIMAL(10, 2),
ADD COLUMN IF NOT EXISTS gst_amount DECIMAL(10, 2);
