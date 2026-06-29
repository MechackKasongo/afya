import type { ExamCategory, ExamRequestStatus, ExamUrgency } from '../api/types';

export const examRequestStatusLabels: Record<ExamRequestStatus, string> = {
  PENDING: 'En attente',
  SPECIMEN_COLLECTED: 'Prélèvement effectué',
  RESULTS_AVAILABLE: 'Résultats disponibles',
  POSTPONED: 'Reportée',
};

export const examUrgencyLabels: Record<ExamUrgency, string> = {
  NORMAL: 'Normal',
  URGENT: 'Urgent',
};

export const examCategoryLabels: Record<ExamCategory, string> = {
  BIOLOGY: 'Biologie',
  IMAGING: 'Imagerie',
  BACTERIOLOGY: 'Bactériologie',
  OTHER: 'Autre',
};

export function formatLabInstant(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('fr-FR');
}

export function parseExamParameterNames(parameters: string | null | undefined): string[] {
  if (!parameters?.trim()) return [];
  return parameters
    .split(/[;,]/)
    .map((part) => part.trim())
    .filter((part) => part.length > 0);
}

export function examTypesSummary(names: string[]): string {
  if (names.length === 0) return '—';
  return names.join(', ');
}

/** Prochaine action laborantin selon le statut de la demande. */
export function labNextActionLabel(status: ExamRequestStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Enregistrer le prélèvement';
    case 'SPECIMEN_COLLECTED':
      return 'Saisir les résultats';
    case 'RESULTS_AVAILABLE':
      return 'Consulter les résultats';
    case 'POSTPONED':
      return 'Reportée';
    default:
      return '—';
  }
}

export function labStatusBadgeClass(status: ExamRequestStatus): string {
  switch (status) {
    case 'PENDING':
      return 'lab-status-badge lab-status-badge--pending';
    case 'SPECIMEN_COLLECTED':
      return 'lab-status-badge lab-status-badge--specimen';
    case 'RESULTS_AVAILABLE':
      return 'lab-status-badge lab-status-badge--done';
    case 'POSTPONED':
      return 'lab-status-badge lab-status-badge--postponed';
    default:
      return 'lab-status-badge';
  }
}

/** Libellé court pour la colonne suivi médecin. */
export function labDoctorFollowUpLabel(status: ExamRequestStatus): string {
  switch (status) {
    case 'RESULTS_AVAILABLE':
      return 'Consulter le compte rendu';
    case 'PENDING':
      return 'En attente du laboratoire';
    case 'SPECIMEN_COLLECTED':
      return 'Analyse en cours';
    default:
      return labNextActionLabel(status);
  }
}
