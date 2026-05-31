-- ========================================
-- V019: Align schema for JPA UUID ID fields
-- ========================================
-- Some tables were created with composite primary keys,
-- but JPA entities expect a UUID id column.

-- ----------------------------------------
-- interchange_fees: add UUID id column
-- ----------------------------------------
ALTER TABLE interchange_fees ADD COLUMN id UUID DEFAULT gen_random_uuid();
ALTER TABLE interchange_fees ALTER COLUMN id SET NOT NULL;
ALTER TABLE interchange_fees DROP CONSTRAINT interchange_fees_pkey;
ALTER TABLE interchange_fees ADD PRIMARY KEY (id);
ALTER TABLE interchange_fees ADD CONSTRAINT uq_interchange_fees_brand_card_type_region_mcc UNIQUE (brand, card_type, region, mcc);
