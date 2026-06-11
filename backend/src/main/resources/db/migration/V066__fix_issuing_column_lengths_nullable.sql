-- V066: Fix issuing entity column lengths and nullable to match Java annotations
-- Applied after runtime proof that no NULL data exists in columns changing to NOT NULL

-- cards: align with Card.java annotations
ALTER TABLE cards
  ALTER COLUMN card_account_id SET NOT NULL,
  ALTER COLUMN card_number_hash DROP NOT NULL,
  ALTER COLUMN card_number_hash TYPE VARCHAR(128),
  ALTER COLUMN card_number_suffix DROP NOT NULL,
  ALTER COLUMN card_type DROP NOT NULL,
  ALTER COLUMN card_brand DROP NOT NULL,
  ALTER COLUMN product_code TYPE VARCHAR(32),
  ALTER COLUMN expiry_date DROP NOT NULL,
  ALTER COLUMN cvv_hash TYPE VARCHAR(128),
  ALTER COLUMN status TYPE VARCHAR(30),
  ALTER COLUMN reissue_reason TYPE VARCHAR(255),
  ALTER COLUMN requestor_reference TYPE VARCHAR(64);

-- wallet_tokens: align with WalletToken.java annotations
ALTER TABLE wallet_tokens
  ALTER COLUMN token TYPE VARCHAR(128),
  ALTER COLUMN token_type DROP NOT NULL,
  ALTER COLUMN token_expiry DROP NOT NULL,
  ALTER COLUMN device_id TYPE VARCHAR(255);

-- pin_management: align with PinManagement.java annotations
ALTER TABLE pin_management
  ALTER COLUMN pin_format TYPE VARCHAR(20);
