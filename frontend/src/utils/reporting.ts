export type ReportingPeriod = '7' | '30' | '90';

export function periodToRange(period: ReportingPeriod): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  const days = period === '7' ? 7 : period === '30' ? 30 : 90;
  from.setDate(from.getDate() - days);
  return { from: from.toISOString(), to: to.toISOString() };
}

export function formatNumberFr(value: number): string {
  return new Intl.NumberFormat('fr-FR').format(value);
}

export function formatInstantRangeFr(from: string, to: string): string {
  const opts: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short', year: 'numeric' };
  const fromDate = new Date(from);
  const toDate = new Date(to);
  if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) {
    return 'Période sélectionnée';
  }
  const fmt = new Intl.DateTimeFormat('fr-FR', opts);
  return `${fmt.format(fromDate)} → ${fmt.format(toDate)}`;
}

export function formatAuditDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

const ACTION_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'Connexion',
  LOGIN_FAILED: 'Échec de connexion',
  LOGOUT_SUCCESS: 'Déconnexion',
  USER_CREATED: 'Compte créé',
  USER_UPDATED: 'Compte modifié',
  USER_DELETED: 'Compte supprimé',
  USER_ACTIVATED: 'Compte activé',
  USER_DEACTIVATED: 'Compte désactivé',
  PATIENT_CREATED: 'Patient créé',
  PATIENT_UPDATED: 'Patient modifié',
  PATIENT_DELETED: 'Patient supprimé',
  ADMISSION_CREATED: 'Admission créée',
  ADMISSION_UPDATED: 'Admission modifiée',
  ADMISSION_DISCHARGED: 'Sortie enregistrée',
  ADMISSION_DEATH_DECLARED: 'Décès déclaré',
  EMERGENCY_CREATED: 'Urgence ouverte',
  EMERGENCY_UPDATED: 'Urgence modifiée',
  EMERGENCY_CLOSED: 'Urgence clôturée',
  CONSULTATION_CREATED: 'Consultation créée',
  CONSULTATION_UPDATED: 'Consultation modifiée',
  STAY_OPENED: 'Séjour ouvert',
  STAY_CLOSED: 'Séjour clôturé',
  STAY_TRANSFERRED: 'Séjour transféré',
};

const RESOURCE_LABELS: Record<string, string> = {
  USER: 'Utilisateur',
  PATIENT: 'Patient',
  ADMISSION: 'Admission',
  EMERGENCY_VISIT: 'Urgence',
  CONSULTATION: 'Consultation',
  STAY: 'Séjour',
  MEDICAL_RECORD: 'Dossier médical',
  HOSPITAL_SERVICE: 'Service',
  DEPARTMENT: 'Département',
  BED: 'Lit',
  APPOINTMENT: 'Rendez-vous',
};

export function formatAuditActionLabel(action: string): string {
  const mapped = ACTION_LABELS[action.trim().toUpperCase()];
  if (mapped) return mapped;
  return action
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function formatAuditResource(resourceType: string, resourceId: string | null): string {
  const type = resourceType?.trim() ?? '';
  const label = RESOURCE_LABELS[type.toUpperCase()] ?? type.replace(/_/g, ' ');
  if (!resourceId?.trim()) {
    return label || '—';
  }
  return `${label} #${resourceId}`;
}

export function formatServiceLabel(service: string): string {
  if (!service || service === 'unknown') return '—';
  return service
    .replace(/-service$/, '')
    .replace(/-/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function parseActorRolesFromMetadata(metadataJson: string | null): string[] {
  if (!metadataJson?.trim()) return [];
  try {
    const parsed = JSON.parse(metadataJson) as { actorRoles?: unknown };
    if (!Array.isArray(parsed.actorRoles)) return [];
    return parsed.actorRoles.filter((r): r is string => typeof r === 'string');
  } catch {
    return [];
  }
}

export function formatAuditMetadataHint(metadataJson: string | null): string | null {
  if (!metadataJson?.trim()) return null;
  const trimmed = metadataJson.trim();
  if (trimmed.length > 120) {
    return `${trimmed.slice(0, 117)}…`;
  }
  return trimmed;
}
