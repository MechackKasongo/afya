-- MD-08 — Lien SoinInfirmier → PrescriptionLine (prescriptionLineId)
-- Permet de tracer quelle prescription a déclenché chaque soin infirmier

ALTER TABLE nursing_care_records
    ADD COLUMN IF NOT EXISTS prescription_line_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_nursing_care_prescription_line
    ON nursing_care_records (prescription_line_id);
