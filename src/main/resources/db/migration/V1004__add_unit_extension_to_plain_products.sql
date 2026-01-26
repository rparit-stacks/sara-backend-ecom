-- Add unit_extension column to plain_products table
ALTER TABLE plain_products 
ADD COLUMN IF NOT EXISTS unit_extension VARCHAR(50) DEFAULT 'per meter';

-- Update existing records to have default value
UPDATE plain_products 
SET unit_extension = 'per meter' 
WHERE unit_extension IS NULL;
