-- ========================================
-- V008: Tunisia BIN Ranges & Local Mandates
-- ========================================

-- ----------------------------------------
-- Seed Tunisian participants
-- ----------------------------------------
INSERT INTO participants (id, code, name, type, status, endpoint_url, endpoint_type, supported_protocols, metadata)
VALUES
    (uuid_generate_v4(), 'SIB', 'Société Internationale de Banque', 'ISSUER', 'ACTIVE', 'sib.internal:8010', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"SIBKTN01","bin_prefixes":"5070,4625"}'),
    (uuid_generate_v4(), 'POSTE_TN', 'Poste Tunisienne (e-DINAR)', 'ISSUER', 'ACTIVE', 'poste.internal:8011', 'TCP', ARRAY['ISO8583'], '{"country":"TN","routing_bic":"POSTTN01","bin_prefixes":"6099,6098"}'),
    (uuid_generate_v4(), 'ATB', 'Arab Tunisian Bank', 'ISSUER', 'ACTIVE', 'atb.internal:8012', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"ATBKTN01","bin_prefixes":"5178"}'),
    (uuid_generate_v4(), 'BH', 'Banque de l''Habitat', 'ISSUER', 'ACTIVE', 'bh.internal:8013', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"BHKTTN01","bin_prefixes":"4765"}'),
    (uuid_generate_v4(), 'BNA', 'Banque Nationale Agricole', 'ISSUER', 'ACTIVE', 'bna.internal:8014', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"BNATTN01","bin_prefixes":"4634"}'),
    (uuid_generate_v4(), 'UIB', 'Union Internationale de Banques', 'ISSUER', 'ACTIVE', 'uib.internal:8015', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"UIBKTN01","bin_prefixes":"4321"}'),
    (uuid_generate_v4(), 'AMC', 'Attijari Banque Tunisie', 'ISSUER', 'ACTIVE', 'amc.internal:8016', 'TCP', ARRAY['ISO8583','ISO20022'], '{"country":"TN","routing_bic":"AMCTTN01","bin_prefixes":"4403"}');

-- ----------------------------------------
-- Seed Tunisia BIN tables
-- ----------------------------------------
DO $$
DECLARE
    sib_id UUID;
    poste_id UUID;
    atb_id UUID;
    bh_id UUID;
    bna_id UUID;
    uib_id UUID;
    amc_id UUID;
BEGIN
    SELECT id INTO sib_id FROM participants WHERE code = 'SIB';
    SELECT id INTO poste_id FROM participants WHERE code = 'POSTE_TN';
    SELECT id INTO atb_id FROM participants WHERE code = 'ATB';
    SELECT id INTO bh_id FROM participants WHERE code = 'BH';
    SELECT id INTO bna_id FROM participants WHERE code = 'BNA';
    SELECT id INTO uib_id FROM participants WHERE code = 'UIB';
    SELECT id INTO amc_id FROM participants WHERE code = 'AMC';

    -- Carte Bleue Tunisie (SIB) - Visa
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '507000', 6, sib_id, 'VISA', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '462500', 6, sib_id, 'VISA', 'CREDIT', 'TN', 'TND');

    -- e-DINAR (Poste Tunisienne) - Carte locale, mapped as OTHER/CB
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '609900', 6, poste_id, 'CB', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '609800', 6, poste_id, 'CB', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '609700', 6, poste_id, 'CB', 'DEBIT', 'TN', 'TND'),
        (uuid_generate_v4(), '609600', 6, poste_id, 'CB', 'DEBIT', 'TN', 'TND');

    -- ATB Mastercard
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '517800', 6, atb_id, 'MASTERCARD', 'CREDIT', 'TN', 'TND');

    -- BH Visa
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '476500', 6, bh_id, 'VISA', 'CREDIT', 'TN', 'TND'),
        (uuid_generate_v4(), '476510', 6, bh_id, 'VISA', 'DEBIT', 'TN', 'TND');

    -- BNA Visa
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '463400', 6, bna_id, 'VISA', 'DEBIT', 'TN', 'TND');

    -- UIB Visa
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '432100', 6, uib_id, 'VISA', 'CREDIT', 'TN', 'TND'),
        (uuid_generate_v4(), '432110', 6, uib_id, 'VISA', 'DEBIT', 'TN', 'TND');

    -- AMC Visa
    INSERT INTO bin_tables (id, bin, bin_length, participant_id, card_brand, card_type, country_code, currency_code)
    VALUES
        (uuid_generate_v4(), '440300', 6, amc_id, 'VISA', 'CREDIT', 'TN', 'TND');
END $$;

-- ----------------------------------------
-- Seed Tunisia routing rules
-- ----------------------------------------
DO $$
DECLARE
    bank_a_id UUID;
    sib_id UUID;
    poste_id UUID;
    atb_id UUID;
    bh_id UUID;
    bna_id UUID;
    uib_id UUID;
    amc_id UUID;
    switch_id UUID;
BEGIN
    SELECT id INTO bank_a_id FROM participants WHERE code = 'BANK_A';
    SELECT id INTO switch_id FROM participants WHERE code = 'SWITCH_MAIN';
    SELECT id INTO sib_id FROM participants WHERE code = 'SIB';
    SELECT id INTO poste_id FROM participants WHERE code = 'POSTE_TN';
    SELECT id INTO atb_id FROM participants WHERE code = 'ATB';
    SELECT id INTO bh_id FROM participants WHERE code = 'BH';
    SELECT id INTO bna_id FROM participants WHERE code = 'BNA';
    SELECT id INTO uib_id FROM participants WHERE code = 'UIB';
    SELECT id INTO amc_id FROM participants WHERE code = 'AMC';

    -- Route Carte Bleue SIB (5070xxx) to SIB
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_CARTE_BLEUE_SIB', 'Route Carte Bleue SIB (5070) transactions to SIB', 100,
            bank_a_id, sib_id,
            '{"operator":"AND","rules":[{"field":"BIN","operator":"STARTS_WITH","value":"5070"},{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'BOTH', '0200', 'ACTIVE');

    -- Route e-DINAR (609xxx) to Poste Tunisienne
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_EDINAR_POSTE', 'Route e-DINAR (609x) transactions to Poste Tunisienne', 100,
            bank_a_id, poste_id,
            '{"operator":"AND","rules":[{"field":"BIN","operator":"STARTS_WITH","value":"609"},{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'ISO8583', '0200', 'ACTIVE');

    -- Route ATB Mastercard (5178) to ATB
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_ATB_MC', 'Route ATB Mastercard (5178) transactions to ATB', 100,
            bank_a_id, atb_id,
            '{"operator":"AND","rules":[{"field":"BIN","operator":"STARTS_WITH","value":"5178"},{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'BOTH', '0200', 'ACTIVE');

    -- Route BH Visa (4765) to BH
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_BH_VISA', 'Route BH Visa (4765) transactions to BH', 100,
            bank_a_id, bh_id,
            '{"operator":"AND","rules":[{"field":"BIN","operator":"STARTS_WITH","value":"4765"},{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'BOTH', '0200', 'ACTIVE');

    -- Route BNA Visa (4634) to BNA
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_BNA_VISA', 'Route BNA Visa (4634) transactions to BNA', 100,
            bank_a_id, bna_id,
            '{"operator":"AND","rules":[{"field":"BIN","operator":"STARTS_WITH","value":"4634"},{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'BOTH', '0200', 'ACTIVE');

    -- Default TND fallback to SWITCH_MAIN
    INSERT INTO routing_rules (id, name, description, priority, source_participant_id, destination_participant_id, condition_expression, protocol, message_type, status)
    VALUES (uuid_generate_v4(), 'TUN_TND_FALLBACK', 'Fallback TND routing to main switch', 9990,
            bank_a_id, switch_id,
            '{"operator":"AND","rules":[{"field":"CURRENCY","operator":"EQUALS","value":"TND"}]}',
            'BOTH', NULL, 'ACTIVE');
END $$;
