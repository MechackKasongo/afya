INSERT INTO departments (code, name, active) VALUES
    ('MED', 'Médecine', TRUE),
    ('CHIR', 'Chirurgie', TRUE),
    ('URG', 'Urgences', TRUE);

INSERT INTO hospital_services (department_id, name, bed_capacity, active)
SELECT d.id, 'Médecine interne', 20, TRUE FROM departments d WHERE d.code = 'MED';

INSERT INTO hospital_services (department_id, name, bed_capacity, active)
SELECT d.id, 'Urgences', 15, TRUE FROM departments d WHERE d.code = 'URG';
