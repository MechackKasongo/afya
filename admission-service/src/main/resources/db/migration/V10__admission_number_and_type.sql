-- MD-05 — Enrichissement de la table admissions
-- Ajout : numéro d'admission lisible, type, et timestamp de mise à jour

ALTER TABLE admissions
    ADD COLUMN IF NOT EXISTS admission_number VARCHAR(20) UNIQUE,
    ADD COLUMN IF NOT EXISTS admission_type   VARCHAR(10) NOT NULL DEFAULT 'NORMALE',
    ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMPTZ;

-- Contrainte sur le type
ALTER TABLE admissions
    ADD CONSTRAINT chk_admission_type CHECK (admission_type IN ('NORMALE', 'URGENCE'));

-- Génération rétroactive des numéros pour les admissions sans numéro
-- Format : ADM-AAAA-NNNNN (ex: ADM-2026-00001)
UPDATE admissions
SET admission_number = 'ADM-' || TO_CHAR(EXTRACT(YEAR FROM admitted_at)::INT, 'FM0000')
                       || '-' || LPAD(id::TEXT, 5, '0')
WHERE admission_number IS NULL;
