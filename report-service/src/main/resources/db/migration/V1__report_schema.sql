-- MD-09 Report Service — schéma initial (métadonnées de rapports matérialisés, évolution future)
CREATE TABLE report_definitions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL UNIQUE,
    label       VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO report_definitions (code, label) VALUES
    ('ACTIVITY', 'Rapport d''activité (agrégats audit)');
