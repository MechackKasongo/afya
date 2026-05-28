/** Affiche les détails d'une prescription (nouveau format ou ancien posologie + fréquence). */
export function formatClinicalPrescriptionDetails(
  dosage: string | null | undefined,
  frequency: string | null | undefined,
): string {
  const d = dosage?.trim();
  const f = frequency?.trim();
  if (!d && !f) {
    return '—';
  }
  if (!f || f === '—') {
    return d ?? '—';
  }
  if (!d) {
    return f;
  }
  return `${d} · ${f}`;
}

export function formatAdmissionPrescriptionDetails(item: {
  dosageText?: string | null;
  frequencyText?: string | null;
  instructionsText?: string | null;
}): string {
  const parts = [item.dosageText, item.frequencyText, item.instructionsText]
    .map((p) => p?.trim())
    .filter(Boolean);
  return parts.length > 0 ? parts.join(' · ') : '—';
}
