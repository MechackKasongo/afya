ALTER TABLE hospital_services
    ADD COLUMN bed_assignment_policy VARCHAR(32) NOT NULL DEFAULT 'ROOM_ORDER_ASC';

ALTER TABLE beds
    ADD COLUMN last_freed_at TIMESTAMP;

-- Lits déjà libres : considérés libres depuis la migration (évite de favoriser artificiellement les nouveaux seuls).
UPDATE beds SET last_freed_at = CURRENT_TIMESTAMP WHERE NOT occupied AND last_freed_at IS NULL;
