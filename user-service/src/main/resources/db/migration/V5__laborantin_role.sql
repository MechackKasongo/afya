INSERT INTO roles (code, label)
VALUES ('LABORANTIN', 'Laborantin(e)')
ON CONFLICT (code) DO NOTHING;
