import type { AdmissionStatus } from '../api/types';

export function isAdmissionClosed(status: AdmissionStatus | null | undefined): boolean {
  return status === 'SORTI' || status === 'DECEDE';
}

export function isAdmissionOpenForAdministrativeActions(status: AdmissionStatus | null | undefined): boolean {
  return status === 'EN_COURS' || status === 'TRANSFERE';
}

export function admissionStatusLabel(status: AdmissionStatus): string {
  switch (status) {
    case 'EN_COURS':
      return 'En cours';
    case 'TRANSFERE':
      return 'Transféré';
    case 'SORTI':
    case 'DECEDE':
      return 'Sorti';
    default:
      return status;
  }
}

export const ADMISSION_CLOSED_MESSAGE = "Ce séjour est clôturé. La saisie n'est plus possible.";
