import type { PatientResponse } from '../api/types';

export type PatientCareStatus = 'DECEASED' | 'HOSPITALIZED' | 'EMERGENCY' | 'DISCHARGED' | 'ACTIVE';

export type PatientCareIndex = {
  hospitalizedPatientIds: Set<number>;
  urgencesPatientIds: Set<number>;
  /** Au moins un séjour clôturé en sortie vivante (SORTI), sans séjour actif. */
  dischargedAlivePatientIds: Set<number>;
};

export function isPatientDeceased(patient: Pick<PatientResponse, 'deceasedAt'> | null | undefined): boolean {
  return Boolean(patient?.deceasedAt);
}

export function resolvePatientCareStatus(
  patientId: number,
  patient: Pick<PatientResponse, 'deceasedAt'> | null | undefined,
  index: PatientCareIndex | null,
): PatientCareStatus {
  if (patient && isPatientDeceased(patient)) {
    return 'DECEASED';
  }
  if (index?.hospitalizedPatientIds.has(patientId)) {
    return 'HOSPITALIZED';
  }
  if (index?.urgencesPatientIds.has(patientId)) {
    return 'EMERGENCY';
  }
  if (index?.dischargedAlivePatientIds.has(patientId)) {
    return 'DISCHARGED';
  }
  return 'ACTIVE';
}

const careStatusLabels: Record<PatientCareStatus, string> = {
  DECEASED: 'Décédé',
  HOSPITALIZED: 'En hospitalisation',
  EMERGENCY: 'Aux urgences',
  DISCHARGED: 'Sorti',
  ACTIVE: 'Actif',
};

export function patientCareStatusLabel(status: PatientCareStatus): string {
  return careStatusLabels[status];
}

export function patientCareStatusBadgeClass(status: PatientCareStatus): string {
  const variant: Record<PatientCareStatus, string> = {
    DECEASED: 'deceased',
    HOSPITALIZED: 'hospitalized',
    EMERGENCY: 'emergency',
    DISCHARGED: 'discharged',
    ACTIVE: 'active',
  };
  return `patient-status-badge patient-status-badge--${variant[status]}`;
}

/** @deprecated Préférer {@link patientCareStatusLabel} avec {@link resolvePatientCareStatus}. */
export function patientStatusLabel(patient: Pick<PatientResponse, 'deceasedAt'> | null | undefined): string {
  return isPatientDeceased(patient) ? 'Décédé' : 'Actif';
}
