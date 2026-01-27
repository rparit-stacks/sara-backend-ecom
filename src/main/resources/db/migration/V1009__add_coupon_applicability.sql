-- Add applicability (GLOBAL or USER_SPECIFIC) and allowed_user_email for user-specific coupons
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS applicability VARCHAR(255) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS allowed_user_email VARCHAR(255) NULL;
