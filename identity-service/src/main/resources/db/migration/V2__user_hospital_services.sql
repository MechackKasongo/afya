CREATE TABLE user_hospital_services (
    user_id             BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    hospital_service_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, hospital_service_id)
);

CREATE INDEX idx_user_hospital_services_service ON user_hospital_services (hospital_service_id);
