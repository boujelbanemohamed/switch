-- V068: Add encrypted card number and CVV columns for viewing card details
-- Encrypted using PciEncryptionService (AES-256-GCM) for PCI compliance

ALTER TABLE cards
  ADD COLUMN IF NOT EXISTS card_number_encrypted VARCHAR(512),
  ADD COLUMN IF NOT EXISTS cvv_encrypted VARCHAR(128);
