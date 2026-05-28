import type { AdmissionClinicalFormResponse, AdmissionClinicalFormUpsertRequest } from '../api/types';

export const CLINICAL_EXAM_OPTIONS = [
  { value: '', label: '— Sélectionner —' },
  { value: 'BIOLOGIE', label: 'Bilan biologique' },
  { value: 'RADIO_THORAX', label: 'Radiographie thorax' },
  { value: 'ECG', label: 'ECG' },
  { value: 'ECHO_ABDOM', label: 'Échographie abdominale' },
  { value: 'ECHO_CARD', label: 'Échographie cardiaque' },
  { value: 'SCANNER', label: 'Scanner' },
  { value: 'IRM', label: 'IRM' },
  { value: 'AUTRE', label: 'Autre (préciser)' },
] as const;

export type ClinicalExamKind = (typeof CLINICAL_EXAM_OPTIONS)[number]['value'];

export type ClinicalExamsFormSlice = {
  examsRequestedKind: ClinicalExamKind;
  examsRequestedOther: string;
  examsDetailText: string;
};

const LEGACY_EXAM_LABELS: { label: string; key: keyof AdmissionClinicalFormResponse }[] = [
  { label: 'Examen pulmonaire', key: 'physicalExamPulmonaryText' },
  { label: 'Examen cardiaque', key: 'physicalExamCardiacText' },
  { label: 'Examen abdominal', key: 'physicalExamAbdominalText' },
  { label: 'Examen neurologique', key: 'physicalExamNeurologicalText' },
];

export function examOptionLabel(value: string): string | null {
  return CLINICAL_EXAM_OPTIONS.find((o) => o.value === value)?.label ?? null;
}

export function mergeExamDetailsFromResponse(data: AdmissionClinicalFormResponse): string {
  const legacyParts = LEGACY_EXAM_LABELS.map(({ label, key }) => {
    const text = data[key];
    if (typeof text !== 'string' || !text.trim()) return null;
    return `${label} :\n${text.trim()}`;
  }).filter(Boolean) as string[];

  const misc = data.physicalExamMiscText?.trim() ?? '';
  if (legacyParts.length === 0) {
    return misc;
  }
  if (!misc) {
    return legacyParts.join('\n\n');
  }
  if (legacyParts.some((part) => misc.includes(part.split(' :\n')[1] ?? ''))) {
    return misc;
  }
  return `${legacyParts.join('\n\n')}\n\n${misc}`;
}

export function parseExamsRequestedFromStorage(stored: string | null | undefined): {
  examsRequestedKind: ClinicalExamKind;
  examsRequestedOther: string;
} {
  const text = stored?.trim() ?? '';
  if (!text) {
    return { examsRequestedKind: '', examsRequestedOther: '' };
  }
  const byLabel = CLINICAL_EXAM_OPTIONS.find((o) => o.value && o.label === text);
  if (byLabel) {
    return { examsRequestedKind: byLabel.value, examsRequestedOther: '' };
  }
  return { examsRequestedKind: 'AUTRE', examsRequestedOther: text };
}

export function formatExamsRequestedForStorage(kind: string, other: string): string | null {
  if (kind === 'AUTRE') {
    const t = other.trim();
    return t || null;
  }
  if (!kind) {
    return null;
  }
  return examOptionLabel(kind) ?? kind;
}

export function examsSliceFromResponse(data: AdmissionClinicalFormResponse): ClinicalExamsFormSlice {
  const { examsRequestedKind, examsRequestedOther } = parseExamsRequestedFromStorage(data.paraclinicalText);
  return {
    examsRequestedKind,
    examsRequestedOther,
    examsDetailText: mergeExamDetailsFromResponse(data),
  };
}

export function applyExamsToUpsertPayload(
  payload: AdmissionClinicalFormUpsertRequest,
  slice: ClinicalExamsFormSlice,
): AdmissionClinicalFormUpsertRequest {
  return {
    ...payload,
    paraclinicalText: formatExamsRequestedForStorage(slice.examsRequestedKind, slice.examsRequestedOther),
    physicalExamPulmonaryText: null,
    physicalExamCardiacText: null,
    physicalExamAbdominalText: null,
    physicalExamNeurologicalText: null,
    physicalExamMiscText: slice.examsDetailText.trim() || null,
  };
}

type ClinicalFormExamFields = Pick<
  AdmissionClinicalFormResponse,
  | 'paraclinicalText'
  | 'physicalExamMiscText'
  | 'physicalExamPulmonaryText'
  | 'physicalExamCardiacText'
  | 'physicalExamAbdominalText'
  | 'physicalExamNeurologicalText'
>;

export function clinicalFormHasExamContent(data: ClinicalFormExamFields | null): boolean {
  if (!data) return false;
  return Boolean(
    data.paraclinicalText?.trim() ||
      data.physicalExamMiscText?.trim() ||
      data.physicalExamPulmonaryText?.trim() ||
      data.physicalExamCardiacText?.trim() ||
      data.physicalExamAbdominalText?.trim() ||
      data.physicalExamNeurologicalText?.trim(),
  );
}

export function displayExamsRequested(data: AdmissionClinicalFormResponse): string | null {
  const { examsRequestedKind, examsRequestedOther } = parseExamsRequestedFromStorage(data.paraclinicalText);
  if (examsRequestedKind === 'AUTRE') {
    return examsRequestedOther.trim() || null;
  }
  if (examsRequestedKind) {
    return examOptionLabel(examsRequestedKind);
  }
  return data.paraclinicalText?.trim() || null;
}

export function displayExamsDetail(data: AdmissionClinicalFormResponse): string | null {
  const merged = mergeExamDetailsFromResponse(data);
  return merged.trim() || null;
}
