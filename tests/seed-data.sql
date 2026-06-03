-- ============================================================
-- seed-data.sql — Comprehensive data seeder
-- Populates all tables for API tests
-- Run: PGPASSWORD="${POSTGRES_PASSWORD}" psql -h localhost -U switch_user -d switch_db -f seed-data.sql
-- ============================================================
SET client_min_messages TO WARNING;

-- ─── Reference IDs ────────────────────────────────────────
-- Merchants:
--   M001  → 0f3defba-3d4d-4726-8d10-d06f92372e96
--   MERCH001 → 550e8400-e29b-41d4-a716-446655440000
-- Participants:
--   BANK_A (acquirer)  → 5229b2e3-19db-4768-a1e5-73b577e6d141
--   BANK_B (issuer)    → 10912e3f-b59f-412e-9022-1faff0f672b4
--   SWITCH_MAIN        → 046b8a7b-d6ee-49be-918a-dc75a2b314c8
--   BANK_C             → dd727a80-1635-43ca-9210-85453addee85
-- Auth users:
--   admin    → 8b05df23-a393-40a3-94b2-9cbf9ce7803b
--   operator → b5c5f560-87b3-4cbc-9960-dd3d395ea06f
-- ============================================================

DO $$
DECLARE
    v_bank_a    uuid := '5229b2e3-19db-4768-a1e5-73b577e6d141';
    v_bank_b    uuid := '10912e3f-b59f-412e-9022-1faff0f672b4';
    v_switch    uuid := '046b8a7b-d6ee-49be-918a-dc75a2b314c8';
    v_bank_c    uuid := 'dd727a80-1635-43ca-9210-85453addee85';
    v_merch001  uuid := '550e8400-e29b-41d4-a716-446655440000';
    v_admin     uuid := '8b05df23-a393-40a3-94b2-9cbf9ce7803b';
    v_cardholder uuid;
    v_program   uuid;
    v_product   uuid;
    v_account   uuid;
    v_card      uuid;
    v_authdec   uuid;
    v_clearing  uuid;
    v_fee_sch   uuid;
    v_fraud_rule uuid;
    v_acs_enroll uuid;
    v_acs_auth  uuid;
    v_epg_tx    uuid := '97d1ae09-7bc9-4144-b133-f6360cbbad0c';
BEGIN
    RAISE NOTICE '=== Seeding started ===';

    -- ─── CARD PROGRAMS ────────────────────────────────────
    INSERT INTO card_programs (id, name, description, program_type, status, brand, start_date)
    VALUES (gen_random_uuid(), 'Classic TND Program', 'Standard consumer debit program', 'CONSUMER', 'ACTIVE', 'VISA', '2026-01-01')
    RETURNING id INTO v_program;

    INSERT INTO card_programs (id, name, description, program_type, status, brand, start_date)
    VALUES (gen_random_uuid(), 'Premium Business', 'Corporate business program', 'CORPORATE', 'ACTIVE', 'MASTERCARD', '2026-01-01');

    INSERT INTO card_programs (id, name, description, program_type, status, brand, start_date)
    VALUES (gen_random_uuid(), 'Platinum Rewards', 'Premium rewards program', 'PLATINUM', 'ACTIVE', 'VISA', '2026-03-01');

    RAISE NOTICE '  Card programs: 3 inserted';

    -- ─── CARD PRODUCTS ────────────────────────────────────
    INSERT INTO card_products (id, program_id, name, description, product_code, card_type, card_brand, card_network, status, daily_limit, weekly_limit, monthly_limit, single_txn_limit, currency_code)
    VALUES (gen_random_uuid(), v_program, 'Classic Debit Visa', 'Standard debit card', 'CLS_DBT_V01', 'DEBIT', 'VISA', 'VISA_NET', 'ACTIVE', 5000, 15000, 50000, 2000, 'TND')
    RETURNING id INTO v_product;

    INSERT INTO card_products (id, program_id, name, description, product_code, card_type, card_brand, card_network, status, currency_code)
    VALUES (gen_random_uuid(), v_program, 'Prepaid Travel', 'Prepaid travel card', 'PRE_TRL_V01', 'PREPAID', 'MASTERCARD', 'MASTERCARD_NET', 'ACTIVE', 'TND');

    RAISE NOTICE '  Card products: 2 inserted';

    -- ─── CARDHOLDER ───────────────────────────────────────
    INSERT INTO cardholders (id, external_id, title, first_name, last_name, date_of_birth, email, phone, mobile, address_line1, city, postal_code, country_code, nationality, id_document_type, id_document_number, status, kyc_level, risk_profile)
    VALUES (gen_random_uuid(), 'EXT_CH_001', 'Mr', 'Ahmed', 'Ben Ali', '1990-05-15', 'ahmed.benali@email.com', '+21650123456', '+21650123456', '123 Avenue Habib Bourguiba', 'Tunis', '1001', 'TN', 'TN', 'NATIONAL_ID', 'ID123456', 'ACTIVE', 3, 'LOW')
    RETURNING id INTO v_cardholder;

    RAISE NOTICE '  Cardholder: 1 inserted';

    -- ─── CARD ACCOUNT ─────────────────────────────────────
    INSERT INTO card_accounts (id, cardholder_id, account_number, iban, account_type, currency_code, balance, ledger_balance, available_balance, hold_amount, status)
    VALUES (gen_random_uuid(), v_cardholder, 'TN5901234567890123456789', 'TN5901234567890123456789', 'CHECKING', 'TND', 15000.000, 15500.000, 14800.000, 500.000, 'ACTIVE')
    RETURNING id INTO v_account;

    INSERT INTO card_accounts (id, cardholder_id, account_number, iban, account_type, currency_code, balance, ledger_balance, available_balance, hold_amount, status)
    VALUES (gen_random_uuid(), v_cardholder, 'SAV001234567890123456', 'TN59SAV001234567890123456', 'SAVINGS', 'TND', 50000.000, 50000.000, 50000.000, 0.000, 'ACTIVE');

    RAISE NOTICE '  Card accounts: 2 inserted';

    -- ─── CARDS ────────────────────────────────────────────
    INSERT INTO cards (id, cardholder_id, card_account_id, card_number_hash, card_number_suffix, card_type, card_brand, card_network, product_code, expiry_date, pin_attempts, pin_max_attempts, status, daily_limit, weekly_limit, monthly_limit, single_txn_limit, contactless_enabled, online_enabled, international_enabled, ecommerce_enabled, atm_enabled)
    VALUES (gen_random_uuid(), v_cardholder, v_account, 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b', '1234', 'DEBIT', 'VISA', 'VISA_NET', 'CLS_DBT_V01', '2029-12-31', 0, 3, 'ACTIVE', 5000, 15000, 50000, 2000, true, true, true, true, true)
    RETURNING id INTO v_card;

    RAISE NOTICE '  Cards: 1 inserted';

    -- ─── VIRTUAL CARDS ────────────────────────────────────
    INSERT INTO virtual_cards (id, funding_card_id, card_product_id, cardholder_id, external_id, pan_hash, pan_suffix, expiry_date, status, usage_type, name_on_card, amount_limit, currency_code, merchant_locked, max_transactions, single_use_amount)
    VALUES (gen_random_uuid(), v_card, v_product, v_cardholder, 'VC_001', 'vpan_hash_001_abcdef1234567890abcdef1234567890abcdef12', '5678', '2027-06-30', 'ACTIVE', 'MULTI_USE', 'Ahmed Ben Ali', 2000.000, 'TND', NULL, 50, NULL);

    INSERT INTO virtual_cards (id, funding_card_id, card_product_id, cardholder_id, external_id, pan_hash, pan_suffix, expiry_date, status, usage_type, name_on_card, amount_limit, currency_code, merchant_locked, max_transactions, single_use_amount)
    VALUES (gen_random_uuid(), v_card, v_product, v_cardholder, 'VC_002', 'vpan_hash_002_234567890abcdef1234567890abcdef1234567890', '9012', '2027-08-15', 'ACTIVE', 'SINGLE_USE', 'Ahmed Ben Ali', 500.000, 'TND', 'Amazon Online', 1, 500.000);

    RAISE NOTICE '  Virtual cards: 2 inserted';

    -- ─── TOKEN VAULT ──────────────────────────────────────
    INSERT INTO token_vault (card_id, fpan_suffix, dpan, dpan_suffix, wallet_provider, device_id, status)
    VALUES (v_card, '1234', '5200000000004321', '4321', 'APPLE_PAY', 'device_iphone_001', 'ACTIVE');

    INSERT INTO token_vault (card_id, fpan_suffix, dpan, dpan_suffix, wallet_provider, device_id, status)
    VALUES (v_card, '1234', '5200000000008765', '8765', 'GOOGLE_PAY', 'device_android_001', 'ACTIVE');

    RAISE NOTICE '  Token vault: 2 inserted';

    -- ─── NOTIFICATION PREFERENCES ─────────────────────────
    INSERT INTO notification_preferences (id, user_id, channel, enabled, contact_value, event_types)
    VALUES (gen_random_uuid(), v_admin, 'EMAIL', true, 'admin@switchplatform.com', 'AUTH_DECLINED,FRAUD_ALERT,SETTLEMENT_REPORT');

    INSERT INTO notification_preferences (id, user_id, channel, enabled, contact_value, event_types)
    VALUES (gen_random_uuid(), v_admin, 'SMS', true, '+21650123456', 'FRAUD_ALERT,HIGH_VALUE_TX');

    RAISE NOTICE '  Notification prefs: 2 inserted';

    -- ─── KYC DOCUMENTS ────────────────────────────────────
    INSERT INTO kyc_documents (cardholder_id, document_type, document_number, issuing_country, expiry_date, file_path, verification_status, verified_by, verified_at, mime_type, file_size, notes)
    VALUES (v_cardholder, 'NATIONAL_ID', 'ID123456', 'TN', '2030-12-31', '/docs/kyc/id123456_front.jpg', 'VERIFIED', 'admin', now() - interval '30 days', 'image/jpeg', 245760, 'Front face of national ID');

    INSERT INTO kyc_documents (cardholder_id, document_type, document_number, issuing_country, expiry_date, file_path, verification_status, mime_type, file_size, notes)
    VALUES (v_cardholder, 'NATIONAL_ID', 'ID123456', 'TN', '2030-12-31', '/docs/kyc/id123456_back.jpg', 'VERIFIED', 'image/jpeg', 198400, 'Back face of national ID');

    INSERT INTO kyc_documents (cardholder_id, document_type, document_number, issuing_country, expiry_date, file_path, verification_status, verified_by, verified_at, mime_type, file_size, notes)
    VALUES (v_cardholder, 'PASSPORT', 'TN1234567', 'TN', '2028-06-15', '/docs/kyc/passport_tn1234567.pdf', 'VERIFIED', 'admin', now() - interval '25 days', 'application/pdf', 512000, 'Passport copy');

    RAISE NOTICE '  KYC documents: 3 inserted';

    -- ─── KYC VERIFICATIONS ────────────────────────────────
    INSERT INTO kyc_verifications (cardholder_id, verification_type, status, requested_level, verified_by, verified_at, notes, metadata)
    VALUES (v_cardholder, 'IDENTITY', 'VERIFIED', 1, 'admin', now() - interval '30 days', 'Identity verified via national ID', '{"method":"automatic","confidence":0.95}'::jsonb);

    INSERT INTO kyc_verifications (cardholder_id, verification_type, status, requested_level, verified_by, verified_at, notes, metadata)
    VALUES (v_cardholder, 'ADDRESS', 'VERIFIED', 2, 'admin', now() - interval '28 days', 'Address verified via utility bill', '{"method":"manual","document_id":"bill_001"}'::jsonb);

    INSERT INTO kyc_verifications (cardholder_id, verification_type, status, requested_level, notes, metadata)
    VALUES (v_cardholder, 'INCOME', 'PENDING', 3, 'Awaiting income verification documents', '{"requested_amount":5000}'::jsonb);

    RAISE NOTICE '  KYC verifications: 3 inserted';

    -- ─── BEHAVIORAL PROFILES ──────────────────────────────
    INSERT INTO behavioral_profiles (cardholder_id, avg_transaction_amount, avg_transactions_per_day, typical_merchant_categories, typical_countries, typical_hours, typical_days, profile_data, model_version, risk_score)
    VALUES (v_cardholder, 85.500, 2.30, '{5812,5411,5311}', '{TN,TN,TN}', '{9,12,14,18}', '{1,2,3,4,5}', '{"avg_merchant_diversity":3,"weekend_ratio":0.15}'::jsonb, 'v2.1', 15);

    RAISE NOTICE '  Behavioral profiles: 1 inserted';

    -- ─── FEE SCHEDULES ────────────────────────────────────
    INSERT INTO fee_schedules (id, name, description, schedule_type, status, priority, currency_code, effective_from, effective_until, participant_id, applies_to)
    VALUES (gen_random_uuid(), 'Standard Interchange TND', 'Standard interchange fees for domestic TND transactions', 'INTERCHANGE', 'ACTIVE', 100, 'TND', '2026-01-01', '2026-12-31', v_bank_a, 'ALL')
    RETURNING id INTO v_fee_sch;

    INSERT INTO fee_schedules (id, name, description, schedule_type, status, priority, currency_code, effective_from, participant_id, applies_to)
    VALUES (gen_random_uuid(), 'Cross Border EUR', 'Cross-border processing fees for EUR', 'CROSS_BORDER', 'ACTIVE', 200, 'EUR', '2026-01-01', v_bank_b, 'ISSUER');

    INSERT INTO fee_schedules (id, name, description, schedule_type, status, priority, currency_code, effective_from, applies_to)
    VALUES (gen_random_uuid(), 'ATM Fee Schedule', 'ATM withdrawal fees', 'ATM', 'ACTIVE', 50, 'TND', '2026-01-01', 'ALL');

    RAISE NOTICE '  Fee schedules: 3 inserted';

    -- ─── FEE RULES ────────────────────────────────────────
    INSERT INTO fee_rules (schedule_id, rule_name, rule_order, calc_method, flat_amount, percentage_rate, min_amount, max_amount, min_tx_amount, max_tx_amount, brand_filter, card_type_filter, mcc_filter, region_filter, entry_mode_filter, is_waivable, description)
    VALUES (v_fee_sch, 'Domestic Debit Chip', 1, 'PERCENTAGE', NULL, 0.005000, 0.100, 5.000, NULL, NULL, 'VISA', 'DEBIT', NULL, 'TUN', 'CHIP', false, '0.5% domestic debit chip transaction');

    INSERT INTO fee_rules (schedule_id, rule_name, rule_order, calc_method, flat_amount, percentage_rate, min_amount, max_amount, min_tx_amount, max_tx_amount, brand_filter, card_type_filter, region_filter, is_waivable, description)
    VALUES (v_fee_sch, 'International Credit', 2, 'MIXED', 0.500, 0.010000, 0.500, 15.000, NULL, NULL, 'MASTERCARD', 'CREDIT', 'INTL', true, '€0.50 + 1% for international credit');

    INSERT INTO fee_rules (schedule_id, rule_name, rule_order, calc_method, flat_amount, min_amount, max_amount, description)
    VALUES (v_fee_sch, 'ATM Flat Fee', 3, 'FLAT', 1.000, 1.000, 1.000, 'Flat TND 1.000 ATM fee');

    RAISE NOTICE '  Fee rules: 3 inserted';

    -- ─── CLEARING RECORDS ─────────────────────────────────
    INSERT INTO clearing_records (id, clearing_date, batch_number, transaction_id, acquiring_participant_id, issuing_participant_id, pan_hash, amount, currency_code, interchange_amount, fee_amount, net_amount, message_type, status)
    VALUES (gen_random_uuid(), '2026-06-03', 'BATCH-20260603-001', 'TX_001_CLR', v_bank_a, v_bank_b, 'pan_hash_001', 150.000, 'TND', 0.750, 0.250, 149.000, '0200', 'CLEARED')
    RETURNING id INTO v_clearing;

    INSERT INTO clearing_records (clearing_date, batch_number, transaction_id, acquiring_participant_id, issuing_participant_id, pan_hash, amount, currency_code, interchange_amount, fee_amount, net_amount, message_type, status)
    VALUES ('2026-06-03', 'BATCH-20260603-001', 'TX_002_CLR', v_bank_a, v_bank_b, 'pan_hash_002', 2500.000, 'TND', 12.500, 1.500, 2486.000, '0200', 'CLEARED');

    INSERT INTO clearing_records (clearing_date, batch_number, transaction_id, acquiring_participant_id, issuing_participant_id, pan_hash, amount, currency_code, interchange_amount, fee_amount, net_amount, message_type, status)
    VALUES ('2026-06-02', 'BATCH-20260602-001', 'TX_003_CLR', v_bank_c, v_bank_b, 'pan_hash_003', 89.900, 'EUR', 0.450, 0.100, 89.350, '0200', 'SETTLED');

    RAISE NOTICE '  Clearing records: 3 inserted';

    -- ─── NETTING RECORDS ──────────────────────────────────
    INSERT INTO netting_records (netting_date, participant_id, counterparty_id, total_sent, total_received, net_amount, currency_code, transaction_count, status)
    VALUES ('2026-06-03', v_bank_a, v_bank_b, 15000.000, 12000.000, 3000.000, 'TND', 45, 'SETTLED');

    INSERT INTO netting_records (netting_date, participant_id, counterparty_id, total_sent, total_received, net_amount, currency_code, transaction_count, status)
    VALUES ('2026-06-03', v_bank_b, v_bank_a, 12000.000, 15000.000, -3000.000, 'TND', 38, 'SETTLED');

    INSERT INTO netting_records (netting_date, participant_id, counterparty_id, total_sent, total_received, net_amount, currency_code, transaction_count, status)
    VALUES ('2026-06-03', v_bank_c, v_bank_b, 5000.000, 3200.000, 1800.000, 'EUR', 12, 'CONFIRMED');

    RAISE NOTICE '  Netting records: 3 inserted';

    -- ─── SETTLEMENTS ──────────────────────────────────────
    INSERT INTO settlements (settlement_date, participant_id, currency_code, total_count, total_amount, fee_amount, net_amount, status)
    VALUES ('2026-06-03', v_bank_a, 'TND', 120, 45000.000, 2250.000, 42750.000, 'COMPLETED');

    INSERT INTO settlements (settlement_date, participant_id, currency_code, total_count, total_amount, fee_amount, net_amount, status)
    VALUES ('2026-06-03', v_bank_b, 'TND', 85, 32000.000, 1600.000, 30400.000, 'COMPLETED');

    INSERT INTO settlements (settlement_date, participant_id, currency_code, total_count, total_amount, fee_amount, net_amount, status)
    VALUES ('2026-06-03', v_bank_c, 'EUR', 25, 12000.000, 600.000, 11400.000, 'IN_PROGRESS');

    RAISE NOTICE '  Settlements: 3 inserted';

    -- ─── SETTLEMENT RECORDS (merchant-level) ──────────────
    INSERT INTO settlement_records (merchant_id, settlement_date, total_amount, total_fee, net_amount, currency_code, status)
    VALUES (v_merch001, '2026-06-03', 8500.0000, 425.0000, 8075.0000, 'TND', 'PENDING');

    RAISE NOTICE '  Settlement records: 1 inserted';

    -- ─── MERCHANT TRANSACTIONS ────────────────────────────
    INSERT INTO merchant_transactions (merchant_id, card_brand, card_type, amount, currency_code, fee_amount, commission_amount, mdr_rate, transaction_date)
    VALUES (v_merch001, 'VISA', 'DEBIT', 150.000, 'TND', 0.750, 0.750, 0.0050, now() - interval '2 hours');

    INSERT INTO merchant_transactions (merchant_id, card_brand, card_type, amount, currency_code, fee_amount, commission_amount, mdr_rate, transaction_date)
    VALUES (v_merch001, 'MASTERCARD', 'CREDIT', 2500.000, 'TND', 25.000, 12.500, 0.0100, now() - interval '1 hour');

    INSERT INTO merchant_transactions (merchant_id, card_brand, card_type, amount, currency_code, fee_amount, commission_amount, mdr_rate, transaction_date)
    VALUES (v_merch001, 'VISA', 'DEBIT', 89.500, 'TND', 0.448, 0.448, 0.0050, now() - interval '30 minutes');

    RAISE NOTICE '  Merchant transactions: 3 inserted';

    -- ─── MERCHANT SETTLEMENTS ─────────────────────────────
    INSERT INTO merchant_settlements (merchant_id, settlement_date, currency_code, total_transactions, total_amount, total_fees, total_commission, net_amount, batch_number, status)
    VALUES (v_merch001, '2026-06-03', 'TND', 25, 8500.000, 425.000, 42.500, 8075.000, 'BATCH-M001-20260603', 'CONFIRMED');

    INSERT INTO merchant_settlements (merchant_id, settlement_date, currency_code, total_transactions, total_amount, total_fees, total_commission, net_amount, batch_number, status)
    VALUES (v_merch001, '2026-06-02', 'TND', 18, 6200.000, 310.000, 31.000, 5890.000, 'BATCH-M001-20260602', 'PAID');

    RAISE NOTICE '  Merchant settlements: 2 inserted';

    -- ─── SWITCH TRANSACTIONS ──────────────────────────────
    INSERT INTO transactions (transaction_id, message_type, protocol, stan, rrn, pan_hash, amount, currency_code, merchant_id, terminal_id, acquiring_participant_id, issuing_participant_id, source_participant_id, destination_participant_id, status, response_code, processing_time_ms, request_at, response_at)
    VALUES ('SW_TX_20260603_001', '0200', 'ISO8583', '000001', '100000000001', 'pan_hash_sw_001', 150.000, 'TND', 'MERCH001', 'TERM001', v_bank_a, v_bank_b, v_bank_a, v_bank_b, 'COMPLETED', '00', 45, now() - interval '3 hours', now() - interval '2 hours');

    INSERT INTO transactions (transaction_id, message_type, protocol, stan, rrn, pan_hash, amount, currency_code, merchant_id, terminal_id, acquiring_participant_id, issuing_participant_id, source_participant_id, destination_participant_id, status, response_code, processing_time_ms, request_at, response_at)
    VALUES ('SW_TX_20260603_002', '0200', 'ISO8583', '000002', '100000000002', 'pan_hash_sw_002', 2500.000, 'TND', 'MERCH001', 'TERM001', v_bank_a, v_bank_b, v_bank_a, v_bank_b, 'COMPLETED', '00', 62, now() - interval '2 hours', now() - interval '1 hour');

    INSERT INTO transactions (transaction_id, message_type, protocol, stan, rrn, pan_hash, amount, currency_code, merchant_id, terminal_id, acquiring_participant_id, issuing_participant_id, source_participant_id, destination_participant_id, status, response_code, processing_time_ms, request_at)
    VALUES ('SW_TX_20260603_003', '0100', 'ISO8583', '000003', '100000000003', 'pan_hash_sw_003', 500.000, 'EUR', 'MERCH001', 'TERM002', v_bank_c, v_bank_b, v_bank_c, v_bank_b, 'PENDING', NULL, NULL, now() - interval '15 minutes');

    RAISE NOTICE '  Switch transactions: 3 inserted';

    -- accounting_transactions requires journal_entries FK; skipped for now

    -- ─── FRAUD RULES ──────────────────────────────────────
    INSERT INTO fraud_rules (id, name, description, rule_category, severity, action, condition_expression, score_weight, cooldown_seconds, status)
    VALUES (gen_random_uuid(), 'High Velocity Alert', 'Flag transactions when count exceeds 10 in 5 minutes', 'VELOCITY', 'HIGH', 'FLAG', '{"field":"tx_count_5min","operator":"GREATER_THAN","value":10}'::jsonb, 70, 600, 'ACTIVE')
    RETURNING id INTO v_fraud_rule;

    INSERT INTO fraud_rules (id, name, description, rule_category, severity, action, condition_expression, score_weight, status)
    VALUES (gen_random_uuid(), 'Geo Anomaly', 'Transaction from unusual country', 'GEO', 'CRITICAL', 'CHALLENGE', '{"field":"country","operator":"NOT_IN","value":"typical_countries"}'::jsonb, 90, 'ACTIVE');

    INSERT INTO fraud_rules (id, name, description, rule_category, severity, action, condition_expression, score_weight, status)
    VALUES (gen_random_uuid(), 'Large Amount', 'Single transaction over threshold', 'AMOUNT', 'MEDIUM', 'BLOCK', '{"field":"amount","operator":"GREATER_THAN","value":5000}'::jsonb, 80, 'ACTIVE');

    RAISE NOTICE '  Fraud rules: 3 inserted';

    -- ─── FRAUD ALERTS ─────────────────────────────────────
    INSERT INTO fraud_alerts (card_id, cardholder_id, transaction_id, rule_id, alert_type, severity, score, description, status, assigned_to)
    VALUES (v_card, v_cardholder, 'SW_TX_20260603_001', v_fraud_rule, 'VELOCITY', 'HIGH', 75, 'Velocity check triggered: 12 tx in 5 min', 'OPEN', 'operator');

    INSERT INTO fraud_alerts (card_id, cardholder_id, transaction_id, rule_id, alert_type, severity, score, description, status, assigned_to)
    VALUES (v_card, v_cardholder, 'SW_TX_20260603_002', v_fraud_rule, 'GEO', 'CRITICAL', 92, 'Transaction from unexpected country: US', 'INVESTIGATING', 'admin');

    INSERT INTO fraud_alerts (card_id, cardholder_id, transaction_id, rule_id, alert_type, severity, score, description, status, decision, resolved_at)
    VALUES (v_card, v_cardholder, 'SW_TX_20260603_001', v_fraud_rule, 'AMOUNT', 'MEDIUM', 60, 'Large amount flagged: 2500 TND', 'CONFIRMED', 'APPROVED', now() - interval '1 day');

    RAISE NOTICE '  Fraud alerts: 3 inserted';

    -- ─── AUTHORIZATION DECISIONS ──────────────────────────
    INSERT INTO auth_decisions (card_id, cardholder_id, rule_id, transaction_id, decision, amount, currency_code, merchant_id, terminal_id, response_code, response_reason, processing_time_ms, decided_at)
    VALUES (v_card, v_cardholder, (SELECT id FROM auth_rules WHERE name = 'Block High Amount' LIMIT 1), 'AUTH_DEC_TX_001', 'DECLINED', 5500.000, 'TND', 'MERCH001', 'TERM001', '51', 'Exceeds limit', 12, now() - interval '4 hours');

    INSERT INTO auth_decisions (card_id, cardholder_id, rule_id, transaction_id, decision, amount, currency_code, merchant_id, terminal_id, response_code, response_reason, processing_time_ms, decided_at)
    VALUES (v_card, v_cardholder, (SELECT id FROM auth_rules WHERE name = 'test' LIMIT 1), 'AUTH_DEC_TX_002', 'APPROVED', 85.500, 'TND', 'MERCH001', 'TERM001', '00', 'Approved', 8, now() - interval '3 hours');

    RAISE NOTICE '  Auth decisions: 2 inserted';

    -- ─── DISPUTES ─────────────────────────────────────────
    INSERT INTO disputes (dispute_number, transaction_id, clearing_record_id, merchant_id, acquiring_participant_id, issuing_participant_id, amount, currency_code, dispute_type, status, reason_code, reason_description, evidence_deadline, resolution_deadline, initiated_by, initiated_at)
    VALUES ('DSP-2026-0001', 'SW_TX_20260603_001', v_clearing, v_merch001, v_bank_a, v_bank_b, 150.000, 'TND', 'NOT_RECEIVED', 'UNDER_REVIEW', 'NR01', 'Cardholder claims merchandise not received', now() + interval '30 days', now() + interval '45 days', 'CARDHOLDER', now() - interval '5 days');

    INSERT INTO disputes (dispute_number, transaction_id, merchant_id, acquiring_participant_id, issuing_participant_id, amount, currency_code, dispute_type, status, reason_code, reason_description, initiated_by, initiated_at)
    VALUES ('DSP-2026-0002', 'SW_TX_20260603_002', v_merch001, v_bank_a, v_bank_b, 2500.000, 'TND', 'FRAUD', 'OPEN', 'FR01', 'Cardholder disputes this transaction as fraudulent', 'CARDHOLDER', now() - interval '1 day');

    RAISE NOTICE '  Disputes: 2 inserted';

    -- ─── ACS / 3DS ────────────────────────────────────────
    -- ACS Enrollment
    INSERT INTO acs_card_enrollments (id, card_id, cardholder_id, merchant_id, status, enrolled_at, card_number_hash, card_brand, card_type, phone_number, email)
    VALUES (gen_random_uuid(), v_card, v_cardholder, v_merch001, 'ACTIVE', now() - interval '60 days', 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b', 'VISA', 'DEBIT', '+21650123456', 'ahmed.benali@email.com')
    RETURNING id INTO v_acs_enroll;

    INSERT INTO acs_card_enrollments (card_id, status, enrolled_at, card_brand, card_type)
    VALUES (v_card, 'ENROLLED', now() - interval '30 days', 'MASTERCARD', 'CREDIT');

    RAISE NOTICE '  ACS enrollments: 2 inserted';

    -- ACS Authentication
    INSERT INTO acs_authentications (id, transaction_id, card_id, merchant_id, merchant_name, amount, currency_code, pan_hash, card_type, card_brand, status, authentication_value, eci, three_ds_version, ds_trans_id, ds_url, acs_url)
    VALUES (gen_random_uuid(), 'ACS_AUTH_TX_001', v_card, v_merch001, 'Test Merchant', 150.000, 'TND', 'pan_hash_acs_001', 'DEBIT', 'VISA', 'AUTHENTICATED', 'AAABBCCDDEEFF==', '05', '2.2.0', '11111111-2222-3333-4444-555555555555', 'https://ds.test/ds', 'https://acs.test/acs')
    RETURNING id INTO v_acs_auth;

    INSERT INTO acs_authentications (transaction_id, card_id, amount, currency_code, pan_hash, card_type, card_brand, status, three_ds_version)
    VALUES ('ACS_AUTH_TX_002', v_card, 2500.000, 'TND', 'pan_hash_acs_002', 'DEBIT', 'MASTERCARD', 'CHALLENGE_REQUIRED', '2.2.0');

    RAISE NOTICE '  ACS authentications: 2 inserted';

    -- 3DS Sessions (references epg_transaction)
    INSERT INTO three_ds_sessions (epg_transaction_id, transaction_id, card_id, three_ds_version, status, authentication_type, acs_reference_number, ds_reference_number, acs_url, term_url, notification_url, ds_trans_id, acs_trans_id, sdk_trans_id, browser_accept_header, browser_ip, browser_language, browser_color_depth, browser_screen_height, browser_screen_width, browser_timezone_offset, browser_user_agent)
    VALUES (v_epg_tx, '3DS_SESSION_001', v_card, '2.2.0', 'COMPLETED', 'PAYMENT', 'ACS-REF-001', 'DS-REF-001', 'https://acs.test/challenge', 'https://merchant.test/term', 'https://merchant.test/3ds-notify', 'ds-trans-001', 'acs-trans-001', 'sdk-trans-001', 'text/html,application/json', '197.0.0.1', 'fr-TN', '24', 1080, 1920, 60, 'Mozilla/5.0 Chrome/120.0');

    INSERT INTO three_ds_sessions (transaction_id, three_ds_version, status, authentication_type)
    VALUES ('3DS_SESSION_002', '2.2.0', 'CREATED', 'PAYMENT');

    RAISE NOTICE '  3DS sessions: 2 inserted';

    -- ─── BATCH JOBS ───────────────────────────────────────
    INSERT INTO batch_jobs (job_name, job_type, status, scheduled_at, started_at, completed_at, records_processed, records_failed, result_summary, triggered_by)
    VALUES ('EOD Clearing 2026-06-02', 'EOD_CLEARING', 'COMPLETED', now() - interval '24 hours', now() - interval '23 hours', now() - interval '22 hours', 150, 2, '{"batches":3,"total_amount":125000}'::jsonb, 'SCHEDULER');

    INSERT INTO batch_jobs (job_name, job_type, status, scheduled_at, started_at, completed_at, records_processed, records_failed, result_summary, triggered_by)
    VALUES ('Reconciliation 2026-06-02', 'RECONCILIATION', 'COMPLETED', now() - interval '22 hours', now() - interval '21 hours', now() - interval '20 hours', 150, 0, '{"matched":148,"unmatched":2,"discrepancies":0}'::jsonb, 'SCHEDULER');

    INSERT INTO batch_jobs (job_name, job_type, status, scheduled_at, started_at, result_summary, triggered_by)
    VALUES ('EOD Clearing 2026-06-03', 'EOD_CLEARING', 'RUNNING', now() - interval '1 hour', now() - interval '45 minutes', '{"batches_in_progress":2}'::jsonb, 'SCHEDULER');

    INSERT INTO batch_jobs (job_name, job_type, status, scheduled_at, triggered_by)
    VALUES ('BOD Positions 2026-06-04', 'BOD_POSITIONS', 'SCHEDULED', now() + interval '12 hours', 'SCHEDULER');

    RAISE NOTICE '  Batch jobs: 4 inserted';

    -- ─── MONITORING EVENTS ────────────────────────────────
    INSERT INTO monitoring_events (event_type, severity, source, message, metric_name, metric_value, threshold_value, details, acknowledged, acknowledged_by, acknowledged_at)
    VALUES ('QUEUE_DEPTH', 'WARNING', 'switch-mq', 'MQ queue depth above threshold', 'queue_depth', 850, 500, '{"queue":"transaction_queue","host":"localhost"}'::jsonb, true, 'admin', now() - interval '30 minutes');

    INSERT INTO monitoring_events (event_type, severity, source, message, metric_name, metric_value, threshold_value, details, acknowledged)
    VALUES ('TRANSACTION_FAILURE_RATE', 'CRITICAL', 'switch-engine', 'Transaction failure rate exceeds threshold', 'failure_rate', 5.8, 3.0, '{"window_minutes":5,"total":120,"failed":7}'::jsonb, false);

    INSERT INTO monitoring_events (event_type, severity, source, message, metric_name, metric_value, threshold_value, details)
    VALUES ('RESPONSE_TIME', 'INFO', 'switch-engine', 'Average response time normal', 'avg_response_ms', 42, 200, '{"window_minutes":5,"samples":500}'::jsonb);

    RAISE NOTICE '  Monitoring events: 3 inserted';

    -- ─── REPORTS ──────────────────────────────────────────
    INSERT INTO reports (name, description, report_type, parameters, file_format, generated_by, generated_at, status)
    VALUES ('Daily Settlement 2026-06-02', 'Settlement report for 2026-06-02', 'SETTLEMENT', '{"date":"2026-06-02","format":"csv"}'::jsonb, 'CSV', 'admin', now() - interval '1 day', 'COMPLETED');

    INSERT INTO reports (name, description, report_type, parameters, file_format, generated_by, generated_at, status)
    VALUES ('Fraud Analysis Q2', 'Fraud trends Q2 2026', 'FRAUD', '{"quarter":"Q2","year":2026}'::jsonb, 'PDF', 'admin', now() - interval '5 days', 'COMPLETED');

    RAISE NOTICE '  Reports: 2 inserted';

    -- ─── TERMINALS ────────────────────────────────────────
    INSERT INTO terminals (merchant_id, terminal_id, serial_number, terminal_type, manufacturer, model, firmware_version, status, location_name, city, country_code, contactless_supported, chip_supported, mag_stripe_supported)
    VALUES (v_merch001, 'TERM001', 'SN-ABC-001', 'PHYSICAL_TPE', 'Ingenico', 'iWL255', 'v3.2.1', 'ACTIVE', 'Main Store', 'Tunis', 'TN', true, true, true);

    INSERT INTO terminals (merchant_id, terminal_id, serial_number, terminal_type, manufacturer, model, firmware_version, status, location_name, city, country_code, contactless_supported, chip_supported, mag_stripe_supported)
    VALUES (v_merch001, 'TERM002', 'SN-ABC-002', 'ECOMMERCE', 'N/A', 'Virtual Terminal', 'v1.0', 'ACTIVE', 'Online Store', 'Tunis', 'TN', false, false, false);

    INSERT INTO terminals (merchant_id, terminal_id, serial_number, terminal_type, manufacturer, model, firmware_version, status, location_name, city, country_code)
    VALUES ((SELECT id FROM merchants WHERE merchant_id = 'M001'), 'TEST01', 'SN-TEST-001', 'PHYSICAL_TPE', 'Verifone', 'VX820', 'v4.1', 'ACTIVE', 'Test Store', 'Tunis', 'TN');

    RAISE NOTICE '  Terminals: 3 inserted';

    RAISE NOTICE '=== Seeding complete ===';
END;
$$;
