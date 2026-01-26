-- Add display_order column to product_variant_options table
ALTER TABLE product_variant_options 
ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- Add display_order column to plain_product_variant_options table
ALTER TABLE plain_product_variant_options 
ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- Update existing records to set displayOrder based on creation order
-- For product_variant_options
DO $$
DECLARE
    rec RECORD;
    order_num INTEGER;
BEGIN
    FOR rec IN 
        SELECT DISTINCT variant_id FROM product_variant_options ORDER BY variant_id
    LOOP
        order_num := 0;
        FOR rec IN 
            SELECT id FROM product_variant_options 
            WHERE variant_id = rec.variant_id 
            ORDER BY id
        LOOP
            UPDATE product_variant_options 
            SET display_order = order_num 
            WHERE id = rec.id;
            order_num := order_num + 1;
        END LOOP;
    END LOOP;
END $$;

-- For plain_product_variant_options
DO $$
DECLARE
    rec RECORD;
    order_num INTEGER;
BEGIN
    FOR rec IN 
        SELECT DISTINCT variant_id FROM plain_product_variant_options ORDER BY variant_id
    LOOP
        order_num := 0;
        FOR rec IN 
            SELECT id FROM plain_product_variant_options 
            WHERE variant_id = rec.variant_id 
            ORDER BY id
        LOOP
            UPDATE plain_product_variant_options 
            SET display_order = order_num 
            WHERE id = rec.id;
            order_num := order_num + 1;
        END LOOP;
    END LOOP;
END $$;
