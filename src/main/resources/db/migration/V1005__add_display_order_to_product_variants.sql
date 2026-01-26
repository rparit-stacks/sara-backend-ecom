-- Add display_order column to product_variants table
ALTER TABLE product_variants 
ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- Update existing records to set displayOrder based on creation order (using id as proxy)
UPDATE product_variants 
SET display_order = (
    SELECT row_number() OVER (PARTITION BY product_id ORDER BY id) - 1
    FROM product_variants pv2
    WHERE pv2.id = product_variants.id
);

-- If the above doesn't work due to subquery limitations, use a simpler approach
-- Set display_order to a sequential number based on id within each product
DO $$
DECLARE
    rec RECORD;
    order_num INTEGER;
BEGIN
    FOR rec IN 
        SELECT DISTINCT product_id FROM product_variants ORDER BY product_id
    LOOP
        order_num := 0;
        FOR rec IN 
            SELECT id FROM product_variants 
            WHERE product_id = rec.product_id 
            ORDER BY id
        LOOP
            UPDATE product_variants 
            SET display_order = order_num 
            WHERE id = rec.id;
            order_num := order_num + 1;
        END LOOP;
    END LOOP;
END $$;

-- Simpler fallback: just set all to 0 if the above fails
-- UPDATE product_variants SET display_order = 0 WHERE display_order IS NULL;
