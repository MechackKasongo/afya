import type { UrgenceTimelineEventResponse } from '../api/types';

const EVENT_LABELS: Record<string, string> = {
  ARRIVEE: 'Arrivée',
  TRIAGE: 'Triage',
  ORIENTATION: 'Transfert',
  CLOTURE: 'Clôture',
  DECES: 'Décès',
};

export function urgenceTimelineEventLabel(type: string): string {
  return EVENT_LABELS[type] ?? type;
}

export function urgenceTimelineEventDisplayLabel(event: UrgenceTimelineEventResponse): string {
  if (event.type === 'ORIENTATION') {
    const details = (event.details ?? '').toLowerCase();
    if (details.includes('domicile') || details.includes('sortie')) {
      return 'Sortie';
    }
    return 'Transfert';
  }
  return urgenceTimelineEventLabel(event.type);
}
