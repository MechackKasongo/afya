ALTER TABLE hospital_services
    ADD COLUMN beds_per_room INT NOT NULL DEFAULT 1;

ALTER TABLE hospital_services
    ADD CONSTRAINT chk_beds_per_room_positive CHECK (beds_per_room >= 1);
