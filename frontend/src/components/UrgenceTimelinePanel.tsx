import type { UrgenceTimelineEventResponse } from '../api/types';
import { urgenceTimelineEventDisplayLabel } from '../utils/urgenceTimeline';

type Props = {
  events: UrgenceTimelineEventResponse[];
  loading?: boolean;
};

export function UrgenceTimelinePanel({ events, loading }: Props) {
  if (loading) {
    return <p className="loading-block">Chargement du fil d’événements…</p>;
  }

  if (events.length === 0) {
    return (
      <p style={{ margin: 0, color: 'var(--muted)', fontSize: '0.9rem' }}>
        Aucun événement enregistré pour ce passage.
      </p>
    );
  }

  const chronological = [...events].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );

  return (
    <ol className="urgence-timeline" aria-label="Fil d’événements du passage">
      {chronological.map((event) => (
        <li key={`${event.id}-${event.type}-${event.createdAt}`} className="urgence-timeline__item">
          <div className="urgence-timeline__marker" aria-hidden />
          <div className="urgence-timeline__body">
            <div className="urgence-timeline__header">
              <span className={`urgence-timeline__type urgence-timeline__type--${event.type.toLowerCase()}`}>
                {urgenceTimelineEventDisplayLabel(event)}
              </span>
              <time className="urgence-timeline__time" dateTime={event.createdAt}>
                {new Date(event.createdAt).toLocaleString('fr-FR')}
              </time>
            </div>
            {event.details ? <p className="urgence-timeline__details">{event.details}</p> : null}
          </div>
        </li>
      ))}
    </ol>
  );
}
