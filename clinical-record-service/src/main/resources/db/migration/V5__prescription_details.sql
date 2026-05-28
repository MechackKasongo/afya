ALTER TABLE prescription_lines ALTER COLUMN dosage TYPE VARCHAR(500);
ALTER TABLE prescription_lines ALTER COLUMN frequency DROP NOT NULL;
