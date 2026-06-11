-- V067: Fix acquiring entity column lengths and nullable to match Java annotations
-- Applied after runtime proof that no NULL data exists in columns changing to NOT NULL

-- merchants: align with Merchant.java annotations
ALTER TABLE merchants
  ALTER COLUMN status TYPE VARCHAR(30);

-- terminals: align with Terminal.java annotations
ALTER TABLE terminals
  ALTER COLUMN serial_number TYPE VARCHAR(64),
  ALTER COLUMN terminal_type SET NOT NULL;
