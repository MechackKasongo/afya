CREATE TABLE audit_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL UNIQUE,
    occurred_at     TIMESTAMP NOT NULL,
    actor_username  VARCHAR(80) NOT NULL,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(60) NOT NULL,
    resource_id     VARCHAR(80),
    source_service  VARCHAR(40) NOT NULL,
    metadata_json   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events (action);
CREATE INDEX idx_audit_events_actor ON audit_events (actor_username);
CREATE INDEX idx_audit_events_resource ON audit_events (resource_type, resource_id);
