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
