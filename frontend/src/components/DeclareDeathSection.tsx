import { useState } from 'react';
import { declarePatientDeath } from '../api/declareDeath';
import { getApiErrorMessage } from '../api/error';

type DeclareDeathSectionProps = {
  patientId: number;
  admissionId?: number | null;
  contextLabel: string;
  disabled?: boolean;
  onSuccess?: () => void;
  /** Carte imbriquée dans la grille « Actions de séjour » (à côté de la sortie). */
  embedded?: boolean;
  /** Contenu seul, sans carte (à placer dans une carte parente). */
  bare?: boolean;
  /** Classe CSS additionnelle pour le bouton (cas d'alignement UI). */
  bareButtonClassName?: string;
};

export function DeclareDeathSection({
  patientId,
  admissionId,
  contextLabel,
  disabled = false,
  onSuccess,
  embedded = false,
  bare = false,
  bareButtonClassName,
}: DeclareDeathSectionProps) {
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const targetLabel = admissionId != null ? `le séjour ${admissionId}` : 'ce patient';
    if (!window.confirm(`Confirmer la déclaration de décès pour ${targetLabel} ?`)) return;

    setSubmitting(true);
    setError(null);
    try {
      await declarePatientDeath(patientId, { admissionId, note });
      setNote('');
      onSuccess?.();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de déclarer le décès.'));
    } finally {
      setSubmitting(false);
    }
  }

  const title = bare || embedded ? (
    <h4 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Déclarer décès</h4>
  ) : (
    <h3 style={{ marginTop: 0 }}>Déclarer décès</h3>
  );

  const body = (
    <>
      {title}
      {contextLabel ? (
        <p
          style={{
            margin: bare || embedded ? '0 0 0.75rem' : '0.35rem 0 0',
            color: 'var(--muted)',
            fontSize: '0.9rem',
          }}
        >
          {contextLabel}
        </p>
      ) : null}
      {error ? (
        <div className="error-banner" style={{ marginTop: bare || embedded ? 0 : '0.75rem' }}>
          {error}
        </div>
      ) : null}
      <form
        onSubmit={onSubmit}
        style={{ marginTop: bare || embedded ? 0 : '0.85rem', display: 'grid', gap: '0.75rem' }}
      >
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor={`death-note-${patientId}`}>Note (optionnel)</label>
          <input
            id={`death-note-${patientId}`}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Ex. décès constaté en service…"
            disabled={disabled || submitting}
          />
        </div>
        <button
          type="submit"
          className={
            bare
              ? `btn btn-danger btn-sm${bareButtonClassName ? ` ${bareButtonClassName}` : ''}`
              : 'btn btn-danger'
          }
          disabled={disabled || submitting}
        >
          {submitting ? 'Traitement…' : 'Déclarer décès'}
        </button>
      </form>
    </>
  );

  if (bare) {
    return <div className="urgence-death-inline">{body}</div>;
  }

  return (
    <div
      className={embedded ? 'card admission-death-card' : 'card'}
      style={{
        margin: embedded ? 0 : undefined,
        marginBottom: embedded ? undefined : '1rem',
        borderColor: 'rgba(232, 93, 106, 0.35)',
      }}
    >
      {body}
    </div>
  );
}
