ALTER TABLE consultation_events
    ADD COLUMN IF NOT EXISTS disease_type VARCHAR(120);
