ALTER TABLE emergency_visits
    ADD COLUMN priority VARCHAR(5) NOT NULL DEFAULT 'P2';

ALTER TABLE emergency_visits
    ADD COLUMN triage_level VARCHAR(20);

ALTER TABLE emergency_visits
    ADD COLUMN orientation VARCHAR(120);

UPDATE emergency_visits SET status = 'CLOTURE' WHERE status = 'SORTIE';

UPDATE emergency_visits
SET status = 'EN_ATTENTE_TRIAGE'
WHERE status = 'EN_COURS' AND triage_level IS NULL;

CREATE INDEX idx_emergency_status ON emergency_visits(status);
CREATE INDEX idx_emergency_priority ON emergency_visits(priority);
