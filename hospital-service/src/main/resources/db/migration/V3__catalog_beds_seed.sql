-- Lits de démonstration pour la suggestion automatique à l'admission (V2 ne créait que bed_capacity).
INSERT INTO beds (hospital_service_id, label, occupied)
SELECT hs.id, (200 + s.n)::text || '-A', FALSE
FROM hospital_services hs
CROSS JOIN generate_series(1, 10) AS s(n)
WHERE hs.name = 'Médecine interne'
  AND NOT EXISTS (
      SELECT 1 FROM beds b WHERE b.hospital_service_id = hs.id
  );

INSERT INTO beds (hospital_service_id, label, occupied)
SELECT hs.id, (300 + s.n)::text || '-B', FALSE
FROM hospital_services hs
CROSS JOIN generate_series(1, 8) AS s(n)
WHERE hs.name = 'Urgences'
  AND NOT EXISTS (
      SELECT 1 FROM beds b WHERE b.hospital_service_id = hs.id
  );
