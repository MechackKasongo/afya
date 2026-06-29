-- M2 : report (POSTPONED) d'une demande d'examen — motif tracé.
ALTER TABLE exam_requests ADD COLUMN postpone_reason VARCHAR(1000);

-- M7 : comptage efficace des demandes en attente par niveau d'urgence.
CREATE INDEX idx_exam_requests_status_urgency ON exam_requests(status, urgency);
