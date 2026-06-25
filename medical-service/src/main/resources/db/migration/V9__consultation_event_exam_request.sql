ALTER TABLE consultation_events ADD COLUMN exam_request_id BIGINT;

CREATE INDEX idx_consultation_events_exam_request ON consultation_events(exam_request_id);
