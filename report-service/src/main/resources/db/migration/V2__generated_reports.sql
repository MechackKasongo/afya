CREATE TABLE generated_reports (
    id            BIGSERIAL PRIMARY KEY,
    report_code   VARCHAR(40)  NOT NULL,
    format        VARCHAR(10)  NOT NULL,
    period_from   TIMESTAMPTZ  NOT NULL,
    period_to     TIMESTAMPTZ  NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(120) NOT NULL,
    file_size     BIGINT       NOT NULL,
    payload       BYTEA        NOT NULL,
    generated_by  VARCHAR(80)  NOT NULL,
    generated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_generated_reports_generated_at ON generated_reports (generated_at DESC);

INSERT INTO report_definitions (code, label) VALUES
    ('ACTIVITY_EXPORT', 'Export rapport d''activité (PDF / Excel)'),
    ('OPERATIONAL_STATS', 'Statistiques labo et soins')
ON CONFLICT (code) DO NOTHING;
