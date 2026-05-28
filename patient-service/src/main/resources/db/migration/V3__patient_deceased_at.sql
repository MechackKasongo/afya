ALTER TABLE patients ADD COLUMN deceased_at TIMESTAMP;

CREATE INDEX idx_patients_deceased_at ON patients (deceased_at);
