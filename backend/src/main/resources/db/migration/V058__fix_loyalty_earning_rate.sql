UPDATE loyalty_programs SET earning_rate = 1.0 WHERE id = '11111111-1111-1111-1111-111111111111';

UPDATE loyalty_tiers SET earning_multiplier = 1.2
WHERE program_id = '11111111-1111-1111-1111-111111111111' AND name = 'Silver';

UPDATE loyalty_tiers SET earning_multiplier = 1.5
WHERE program_id = '11111111-1111-1111-1111-111111111111' AND name = 'Gold';

UPDATE loyalty_tiers SET earning_multiplier = 2.0
WHERE program_id = '11111111-1111-1111-1111-111111111111' AND name = 'Platinum';
