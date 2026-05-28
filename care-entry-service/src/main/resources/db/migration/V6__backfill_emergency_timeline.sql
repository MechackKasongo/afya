-- Rétro-remplissage pour les passages créés avant la timeline persistée.
INSERT INTO emergency_visit_timeline_events (emergency_visit_id, event_type, details, created_at)
SELECT
    v.id,
    'ARRIVEE',
    'Priorité ' || v.priority || CASE
        WHEN v.triage_notes IS NOT NULL AND TRIM(v.triage_notes) <> '' THEN ' — Motif : ' || TRIM(v.triage_notes)
        ELSE ''
    END,
    v.arrived_at
FROM emergency_visits v
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_visit_timeline_events e
    WHERE e.emergency_visit_id = v.id AND e.event_type = 'ARRIVEE'
);

INSERT INTO emergency_visit_timeline_events (emergency_visit_id, event_type, details, created_at)
SELECT
    v.id,
    'TRIAGE',
    'Niveau ' || TRIM(v.triage_level),
    v.arrived_at + INTERVAL '1 second'
FROM emergency_visits v
WHERE v.triage_level IS NOT NULL AND TRIM(v.triage_level) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM emergency_visit_timeline_events e
    WHERE e.emergency_visit_id = v.id AND e.event_type = 'TRIAGE'
);

INSERT INTO emergency_visit_timeline_events (emergency_visit_id, event_type, details, created_at)
SELECT
    v.id,
    'ORIENTATION',
    TRIM(v.orientation),
    v.arrived_at + INTERVAL '2 seconds'
FROM emergency_visits v
WHERE v.orientation IS NOT NULL AND TRIM(v.orientation) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM emergency_visit_timeline_events e
    WHERE e.emergency_visit_id = v.id AND e.event_type = 'ORIENTATION'
);

INSERT INTO emergency_visit_timeline_events (emergency_visit_id, event_type, details, created_at)
SELECT
    v.id,
    'CLOTURE',
    'Passage clôturé',
    COALESCE(v.ended_at, v.arrived_at + INTERVAL '3 seconds')
FROM emergency_visits v
WHERE v.status = 'CLOTURE'
  AND NOT EXISTS (
    SELECT 1 FROM emergency_visit_timeline_events e
    WHERE e.emergency_visit_id = v.id AND e.event_type = 'CLOTURE'
);
