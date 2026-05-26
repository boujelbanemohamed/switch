-- Seed participants
INSERT INTO participants (id, code, name, type, status, endpoint_url, endpoint_type, supported_protocols, metadata)
VALUES
    (uuid_generate_v4(), 'BANK_A', 'Bank A - Acquirer', 'ACQUIRER', 'ACTIVE', 'bank-a.internal:8001', 'TCP', ARRAY['ISO8583'], '{"country":"TN","routing_bic":"BANKTN01"}'),
    (uuid_generate_v4(), 'BANK_B', 'Bank B - Issuer', 'ISSUER', 'ACTIVE', 'bank-b.internal:8002', 'TCP', ARRAY['ISO8583'], '{"country":"TN","routing_bic":"BANKTN02"}'),
    (uuid_generate_v4(), 'SWITCH_MAIN', 'Main Switch', 'SWITCH', 'ACTIVE', NULL, NULL, ARRAY['ISO8583','ISO20022'], '{"type":"central"}'),
    (uuid_generate_v4(), 'PROC_EU', 'European Processor', 'PROCESSOR', 'ACTIVE', 'https://api.processor-eu.com/switch', 'HTTP', ARRAY['ISO20022'], '{"region":"EU"}'),
    (uuid_generate_v4(), 'BANK_C', 'Bank C - International', 'ISSUER', 'ACTIVE', 'bank-c.internal:8003', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"FR","routing_bic":"BANKCFR01"}');
ON CONFLICT DO NOTHING;

-- Seed BIN tables
DO $$
DECLARE
    bank_a_id UUID;
    bank_b_id UUID;
    bank_c_id UUID;
BEGIN
    SELECT id INTO bank_a_id FROM participants WHERE code = 'BANK_A';
    SELECT id INTO bank_b_id FROM participants WHERE code = 'BANK_B';
    SELECT id INTO bank_c_id FROM participants WHERE code = 'BANK_C';

    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '400000', 6, bank_a_id, 'VISA', 'CREDIT', 'TN', 'TND'),
        (uuid_generate_v4(), '411111', 6, bank_a_id, 'VISA', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '550000', 6, bank_b_id, 'MASTERCARD', 'CREDIT', 'TN', 'TND'),
        (uuid_generate_v4(), '555555', 6, bank_b_id, 'MASTERCARD', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '440000', 6, bank_c_id, 'VISA', 'CREDIT', 'FR', 'EUR'),
        (uuid_generate_v4(), '450000', 6, bank_c_id, 'VISA', 'DEBIT', 'FR', 'EUR');
END $$;

-- Seed routing rules
DO $$
DECLARE
    switch_id UUID;
    bank_a_id UUID;
    bank_b_id UUID;
    bank_c_id UUID;
    proc_eu_id UUID;
BEGIN
    SELECT id INTO switch_id FROM participants WHERE code = 'SWITCH_MAIN';
    SELECT id INTO bank_a_id FROM participants WHERE code = 'BANK_A';
    SELECT id INTO bank_b_id FROM participants WHERE code = 'BANK_B';
    SELECT id INTO bank_c_id FROM participants WHERE code = 'BANK_C';
    SELECT id INTO proc_eu_id FROM participants WHERE code = 'PROC_EU';

    -- Route all transactions from Bank A (acquirer) to Bank B (issuer) for TND currency
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES
        (uuid_generate_v4(), 'BANK_A_TO_BANK_B_TND', 'Route TND transactions from Bank A to Bank B', 100,
         bank_a_id, bank_b_id,
         '{"operator":"AND","rules":[{"field":"CURRENCY","operator":"EQUALS","value":"TND"},{"field":"BIN","operator":"STARTS_WITH","value":"55"}]}',
         'BOTH', '0200', 'ACTIVE');

    -- Route EUR transactions from Bank A to European processor
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES
        (uuid_generate_v4(), 'BANK_A_TO_PROC_EU_EUR', 'Route EUR transactions to European processor', 200,
         bank_a_id, proc_eu_id,
         '{"operator":"AND","rules":[{"field":"CURRENCY","operator":"EQUALS","value":"EUR"}]}',
         'ISO20022', 'pacs.008', 'ACTIVE');

    -- Default fallback route
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES
        (uuid_generate_v4(), 'DEFAULT_TO_SWITCH', 'Default route to main switch', 9999,
         NULL, switch_id,
         '{"operator":"AND","rules":[]}',
         'BOTH', NULL, 'ACTIVE');
END $$;

-- Seed message templates
INSERT INTO message_templates (id, name, mti, protocol, fields_definition)
VALUES
    (uuid_generate_v4(), 'Authorization Request', '0200', 'ISO8583',
     '{"required_fields":[2,3,4,7,11,12,13,22,35,37,41,42,49],"description":"Financial authorization request"}'),
    (uuid_generate_v4(), 'Authorization Response', '0210', 'ISO8583',
     '{"required_fields":[2,3,4,7,11,12,13,37,39,41,42,49],"description":"Financial authorization response"}'),
    (uuid_generate_v4(), 'Financial Presentment', '0100', 'ISO8583',
     '{"required_fields":[2,3,4,7,11,12,13,22,35,37,41,42,49],"description":"Financial presentment request"}'),
    (uuid_generate_v4(), 'Reversal Request', '0400', 'ISO8583',
     '{"required_fields":[2,3,4,7,11,12,37,90],"description":"Reversal/chargeback request"}'),
    (uuid_generate_v4(), 'Credit Transfer (pacs.008)', 'pacs.008', 'ISO20022',
     '{"required_fields":["MsgId","NbOfTxs","IntrBkSttlmAmt","BICFI","IBAN"],"description":"FIToFICstmrCdtTrf"}');
