-- MD-09 — Tables pré-agrégées de statistiques (StatistiqueAdmission + StatistiqueMedical)
-- Alimentées quotidiennement par AdmissionStatsScheduler

-- StatistiqueAdmission : indicateurs journaliers des admissions
CREATE TABLE admission_stats (
    id                       BIGSERIAL    PRIMARY KEY,
    stat_date                DATE         NOT NULL UNIQUE,
    admissions_count         INT          NOT NULL DEFAULT 0,
    discharges_count         INT          NOT NULL DEFAULT 0,
    transfers_count          INT          NOT NULL DEFAULT 0,
    deaths_count             INT          NOT NULL DEFAULT 0,
    active_admissions        INT          NOT NULL DEFAULT 0,
    avg_stay_days            DOUBLE PRECISION,
    occupancy_rate_percent   DOUBLE PRECISION,
    available_beds           INT,
    occupied_beds            INT,
    total_beds               INT,
    computed_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admission_stats_stat_date ON admission_stats (stat_date DESC);

-- StatistiqueMedical : indicateurs journaliers de l'activité médicale
CREATE TABLE medical_stats (
    id                    BIGSERIAL    PRIMARY KEY,
    stat_date             DATE         NOT NULL UNIQUE,
    consultations_count   INT          NOT NULL DEFAULT 0,
    prescriptions_count   INT          NOT NULL DEFAULT 0,
    diagnoses_count       INT          NOT NULL DEFAULT 0,
    exam_requests_count   INT          NOT NULL DEFAULT 0,
    nursing_care_count    INT          NOT NULL DEFAULT 0,
    total_audit_events    INT          NOT NULL DEFAULT 0,
    computed_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medical_stats_stat_date ON medical_stats (stat_date DESC);
