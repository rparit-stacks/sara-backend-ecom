-- Create payment_config table
CREATE TABLE IF NOT EXISTS payment_config (
    id BIGSERIAL PRIMARY KEY,
    razorpay_key_id VARCHAR(255),
    razorpay_key_secret TEXT,
    razorpay_enabled BOOLEAN DEFAULT FALSE,
    stripe_public_key VARCHAR(255),
    stripe_secret_key TEXT,
    stripe_enabled BOOLEAN DEFAULT FALSE,
    cod_enabled BOOLEAN DEFAULT FALSE,
    partial_cod_enabled BOOLEAN DEFAULT FALSE,
    partial_cod_advance_percentage INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Migrate existing payment config data from business_config to payment_config
INSERT INTO payment_config (
    razorpay_key_id,
    razorpay_key_secret,
    razorpay_enabled,
    stripe_public_key,
    stripe_secret_key,
    stripe_enabled,
    cod_enabled,
    partial_cod_enabled,
    partial_cod_advance_percentage,
    created_at,
    updated_at
)
SELECT 
    razorpay_key_id,
    razorpay_key_secret,
    COALESCE(razorpay_enabled, FALSE) as razorpay_enabled,
    stripe_public_key,
    stripe_secret_key,
    COALESCE(stripe_enabled, FALSE) as stripe_enabled,
    COALESCE(cod_enabled, FALSE) as cod_enabled,
    COALESCE(partial_cod_enabled, FALSE) as partial_cod_enabled,
    partial_cod_advance_percentage,
    NOW() as created_at,
    NOW() as updated_at
FROM business_config
WHERE razorpay_key_id IS NOT NULL 
   OR stripe_public_key IS NOT NULL 
   OR cod_enabled = TRUE 
   OR partial_cod_enabled = TRUE
LIMIT 1
ON CONFLICT DO NOTHING;

-- If no data was migrated, create a default payment config
INSERT INTO payment_config (
    razorpay_enabled,
    stripe_enabled,
    cod_enabled,
    partial_cod_enabled,
    created_at,
    updated_at
)
SELECT FALSE, FALSE, FALSE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payment_config);
