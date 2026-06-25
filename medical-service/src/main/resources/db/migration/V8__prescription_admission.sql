ALTER TABLE prescription_lines ADD COLUMN admission_id BIGINT;

CREATE INDEX idx_prescription_lines_admission ON prescription_lines(admission_id);
