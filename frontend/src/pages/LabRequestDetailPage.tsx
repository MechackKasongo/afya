import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import type {
  ExamRequestResponse,
  ExamResultRequest,
  ExamResultResponse,
  ExamTypeResponse,
  PatientResponse,
  ResultParameterRequest,
  SpecimenCollectionRequest,
} from '../api/types';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import {
  examCategoryLabels,
  examRequestStatusLabels,
  examTypesSummary,
  examUrgencyLabels,
  formatLabInstant,
  parseExamParameterNames,
} from '../utils/labDisplay';

type ParamDraft = ResultParameterRequest & { key: string };

function buildParamDrafts(examTypesCatalog: ExamTypeResponse[], request: ExamRequestResponse): ParamDraft[] {
  const names = new Set<string>();
  for (const summary of request.examTypes) {
    const full = examTypesCatalog.find((t) => t.id === summary.id);
    for (const name of parseExamParameterNames(full?.parameters)) {
      names.add(name);
    }
  }
  if (names.size === 0) {
    for (const summary of request.examTypes) {
      names.add(summary.name);
    }
  }
  return [...names].map((name) => ({
    key: name,
    parameterName: name,
    value: '',
    unit: '',
    referenceMin: '',
    referenceMax: '',
    abnormal: false,
  }));
}

export function LabRequestDetailPage() {
  const { id } = useParams();
  const requestId = Number(id);
  const { user } = useAuth();
  const canProcess =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN') || hasRole(user, 'ROLE_INFIRMIER');

  const [request, setRequest] = useState<ExamRequestResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [examTypesCatalog, setExamTypesCatalog] = useState<ExamTypeResponse[]>([]);
  const [result, setResult] = useState<ExamResultResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [sampleType, setSampleType] = useState('Sang');
  const [specimenSubmitting, setSpecimenSubmitting] = useState(false);
  const [specimenError, setSpecimenError] = useState<string | null>(null);

  const [annotation, setAnnotation] = useState('');
  const [paramDrafts, setParamDrafts] = useState<ParamDraft[]>([]);
  const [resultSubmitting, setResultSubmitting] = useState(false);
  const [resultError, setResultError] = useState<string | null>(null);

  async function loadAll() {
    if (!Number.isFinite(requestId)) {
      setError('Identifiant de demande invalide.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [requestRes, typesRes] = await Promise.all([
        api.get<ExamRequestResponse>(`/api/v1/lab/exam-requests/${requestId}`),
        api.get<ExamTypeResponse[]>('/api/v1/lab/exam-types'),
      ]);
      setRequest(requestRes.data);
      setExamTypesCatalog(typesRes.data);
      setParamDrafts(buildParamDrafts(typesRes.data, requestRes.data));

      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${requestRes.data.patientId}`);
      setPatient(patientRes.data);

      if (requestRes.data.status === 'RESULTS_AVAILABLE') {
        try {
          const resultRes = await api.get<ExamResultResponse>(`/api/v1/lab/exam-requests/${requestId}/result`);
          setResult(resultRes.data);
        } catch {
          setResult(null);
        }
      } else {
        setResult(null);
      }
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger la demande.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestId]);

  const patientName = useMemo(() => {
    if (!patient) return request ? String(request.patientId) : '—';
    const name = `${patient.firstName} ${patient.lastName}`.trim();
    return name || `Patient ${patient.id}`;
  }, [patient, request]);

  async function submitSpecimen(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !request) return;
    setSpecimenSubmitting(true);
    setSpecimenError(null);
    const payload: SpecimenCollectionRequest = {
      labTechnicianId: user.id,
      sampleType: sampleType.trim(),
    };
    try {
      await api.post<ExamRequestResponse>(`/api/v1/lab/exam-requests/${request.id}/specimen`, payload);
      await loadAll();
    } catch (err) {
      setSpecimenError(getApiErrorMessage(err, 'Impossible d\'enregistrer le prélèvement.'));
    } finally {
      setSpecimenSubmitting(false);
    }
  }

  function updateParam(key: string, patch: Partial<ParamDraft>) {
    setParamDrafts((prev) => prev.map((row) => (row.key === key ? { ...row, ...patch } : row)));
  }

  async function submitResult(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !request) return;
    const parameters = paramDrafts
      .filter((row) => row.value.trim().length > 0)
      .map(({ parameterName, value, unit, referenceMin, referenceMax, abnormal }) => ({
        parameterName,
        value: value.trim(),
        unit: unit?.trim() || null,
        referenceMin: referenceMin?.trim() || null,
        referenceMax: referenceMax?.trim() || null,
        abnormal,
      }));
    if (parameters.length === 0) {
      setResultError('Saisissez au moins une valeur de résultat.');
      return;
    }
    setResultSubmitting(true);
    setResultError(null);
    const payload: ExamResultRequest = {
      labTechnicianId: user.id,
      annotation: annotation.trim() || null,
      parameters,
    };
    try {
      await api.post<ExamResultResponse>(`/api/v1/lab/exam-requests/${request.id}/result`, payload);
      await loadAll();
    } catch (err) {
      setResultError(getApiErrorMessage(err, 'Impossible d\'enregistrer les résultats.'));
    } finally {
      setResultSubmitting(false);
    }
  }

  if (loading) {
    return <LoadingBlock label="Chargement de la demande…" />;
  }

  if (error || !request) {
    return (
      <div className="page-stack">
        <PageHeader title="Demande d'examen" subtitle="Laboratoire" />
        <p className="form-error">{error ?? 'Demande introuvable.'}</p>
        <Link to="/lab/requests" className="btn btn-ghost">
          Retour à la liste
        </Link>
      </div>
    );
  }

  return (
    <div className="page-stack">
      <PageHeader
        title={`Demande n° ${request.id}`}
        subtitle={`${patientName} — ${examRequestStatusLabels[request.status]}`}
      >
        <Link to="/lab/requests" className="btn btn-ghost">
          Liste des demandes
        </Link>
      </PageHeader>

      <section className="card form-stack">
        <h2 className="card__title">Informations</h2>
        <dl className="detail-grid">
          <div>
            <dt>Patient</dt>
            <dd>{patientName}</dd>
          </div>
          <div>
            <dt>Urgence</dt>
            <dd>{examUrgencyLabels[request.urgency]}</dd>
          </div>
          <div>
            <dt>Statut</dt>
            <dd>{examRequestStatusLabels[request.status]}</dd>
          </div>
          <div>
            <dt>Demandé le</dt>
            <dd>{formatLabInstant(request.requestedAt)}</dd>
          </div>
          <div>
            <dt>Examens</dt>
            <dd>
              <ul>
                {request.examTypes.map((type) => (
                  <li key={type.id}>
                    {type.name} ({examCategoryLabels[type.category]})
                  </li>
                ))}
              </ul>
            </dd>
          </div>
          {request.comment && (
            <div>
              <dt>Commentaire</dt>
              <dd>{request.comment}</dd>
            </div>
          )}
        </dl>
      </section>

      {request.status === 'PENDING' && canProcess && (
        <section className="card form-stack">
          <h2 className="card__title">Enregistrer le prélèvement</h2>
          <form onSubmit={(e) => void submitSpecimen(e)}>
            <label htmlFor="sample-type">Type d&apos;échantillon</label>
            <input
              id="sample-type"
              value={sampleType}
              onChange={(e) => setSampleType(e.target.value)}
              required
            />
            {specimenError && <p className="form-error">{specimenError}</p>}
            <button type="submit" className="btn btn-primary" disabled={specimenSubmitting}>
              {specimenSubmitting ? 'Enregistrement…' : 'Confirmer le prélèvement'}
            </button>
          </form>
        </section>
      )}

      {request.status === 'SPECIMEN_COLLECTED' && canProcess && (
        <section className="card form-stack">
          <h2 className="card__title">Saisir les résultats</h2>
          <p className="hint">
            Paramètres suggérés pour : {examTypesSummary(request.examTypes.map((t) => t.name))}
          </p>
          <form onSubmit={(e) => void submitResult(e)} className="form-stack">
            {paramDrafts.map((row) => (
              <div key={row.key} className="card" style={{ padding: '0.75rem' }}>
                <strong>{row.parameterName}</strong>
                <div className="form-row">
                  <label htmlFor={`value-${row.key}`}>Valeur *</label>
                  <input
                    id={`value-${row.key}`}
                    value={row.value}
                    onChange={(e) => updateParam(row.key, { value: e.target.value })}
                  />
                </div>
                <div className="form-row">
                  <label htmlFor={`unit-${row.key}`}>Unité</label>
                  <input
                    id={`unit-${row.key}`}
                    value={row.unit ?? ''}
                    onChange={(e) => updateParam(row.key, { unit: e.target.value })}
                  />
                </div>
                <label style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={row.abnormal}
                    onChange={(e) => updateParam(row.key, { abnormal: e.target.checked })}
                  />
                  Valeur anormale
                </label>
              </div>
            ))}
            <label htmlFor="result-annotation">Annotation</label>
            <textarea
              id="result-annotation"
              rows={3}
              value={annotation}
              onChange={(e) => setAnnotation(e.target.value)}
            />
            {resultError && <p className="form-error">{resultError}</p>}
            <button type="submit" className="btn btn-primary" disabled={resultSubmitting}>
              {resultSubmitting ? 'Enregistrement…' : 'Publier les résultats'}
            </button>
          </form>
        </section>
      )}

      {request.status === 'RESULTS_AVAILABLE' && result && (
        <section className="card form-stack">
          <h2 className="card__title">Résultats</h2>
          <p className="hint">Enregistrés le {formatLabInstant(result.resultedAt)}</p>
          {result.annotation && <p>{result.annotation}</p>}
          <table className="data-table">
            <thead>
              <tr>
                <th>Paramètre</th>
                <th>Valeur</th>
                <th>Unité</th>
                <th>Réf.</th>
                <th>Anormal</th>
              </tr>
            </thead>
            <tbody>
              {result.parameters.map((param) => (
                <tr key={param.id}>
                  <td>{param.parameterName}</td>
                  <td>{param.value}</td>
                  <td>{param.unit ?? '—'}</td>
                  <td>
                    {param.referenceMin || param.referenceMax
                      ? `${param.referenceMin ?? '—'} – ${param.referenceMax ?? '—'}`
                      : '—'}
                  </td>
                  <td>{param.abnormal ? 'Oui' : 'Non'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
