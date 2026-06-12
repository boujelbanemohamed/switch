-- V069: Add participant_id to cardholders for institution affiliation

ALTER TABLE cardholders
  ADD COLUMN IF NOT EXISTS participant_id UUID REFERENCES participants(id);
