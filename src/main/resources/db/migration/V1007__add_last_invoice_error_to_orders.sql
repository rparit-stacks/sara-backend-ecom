-- Last Swipe invoice error for admin display (our_system vs swipe, message, hint)
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS last_invoice_error_source VARCHAR(32),
ADD COLUMN IF NOT EXISTS last_invoice_error_message TEXT,
ADD COLUMN IF NOT EXISTS last_invoice_error_hint TEXT;
