ALTER TABLE consultation_events
    ADD COLUMN IF NOT EXISTS disease_name VARCHAR(255);

CREATE TABLE IF NOT EXISTS disease_catalog (
    id               BIGSERIAL PRIMARY KEY,
    disease_type     VARCHAR(120) NOT NULL,
    label            VARCHAR(255) NOT NULL,
    label_normalized VARCHAR(255) NOT NULL,
    usage_count      INT NOT NULL DEFAULT 0,
    first_used_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_disease_catalog_type_label UNIQUE (disease_type, label_normalized)
);

CREATE INDEX IF NOT EXISTS idx_disease_catalog_type_usage
    ON disease_catalog (disease_type, usage_count DESC);
