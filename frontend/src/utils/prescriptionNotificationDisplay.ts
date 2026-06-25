import type { PrescriptionNotificationStatus } from '../api/types';

const statusLabels: Record<PrescriptionNotificationStatus, string> = {
  ENVOYEE: 'Envoyée',
  LUE: 'Lue',
  EXECUTEE: 'Exécutée',
};

export function prescriptionNotificationStatusLabel(status: PrescriptionNotificationStatus): string {
  return statusLabels[status] ?? status;
}

export function prescriptionNotificationStatusClass(status: PrescriptionNotificationStatus): string {
  if (status === 'ENVOYEE') return 'prescription-notification-badge prescription-notification-badge--envoyee';
  if (status === 'LUE') return 'prescription-notification-badge prescription-notification-badge--lue';
  return 'prescription-notification-badge prescription-notification-badge--executee';
}

export function formatPrescriptionNotificationDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('fr-FR');
}
