ALTER TABLE admissions
    ADD COLUMN IF NOT EXISTS admission_reason VARCHAR(255);
