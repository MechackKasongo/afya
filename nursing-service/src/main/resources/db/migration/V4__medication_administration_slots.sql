ALTER TABLE medication_administrations DROP CONSTRAINT IF EXISTS medication_administrations_prescription_line_id_key;

ALTER TABLE medication_administrations
    ADD COLUMN administration_date DATE,
    ADD COLUMN slot VARCHAR(20),
    ADD COLUMN administered BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE medication_administrations
SET administration_date = CAST(administered_at AS DATE),
    slot = 'JOURNEE'
WHERE administration_date IS NULL;

ALTER TABLE medication_administrations
    ALTER COLUMN administration_date SET NOT NULL,
    ALTER COLUMN slot SET NOT NULL;

CREATE UNIQUE INDEX uq_medication_admin_line_date_slot
    ON medication_administrations (prescription_line_id, administration_date, slot);

CREATE INDEX idx_medication_admin_prescription ON medication_administrations (prescription_line_id);
