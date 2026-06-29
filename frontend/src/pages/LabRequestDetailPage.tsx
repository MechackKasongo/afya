import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole, isLabPortalUser } from '../auth/roles';
import { LabWorkflowSteps } from '../components/LabWorkflowSteps';
import { ScrollTableRegion } from '../components/ScrollTableRegion';
import type {
  ExamRequestResponse,
  ExamResultRequest,
  ExamResultResponse,
  ExamTypeResponse,
  ExamRequestStatus,
  PageExamRequestResponse,
  PatientResponse,
  ResultParameterRequest,
  ResultParameterResponse,
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
  labNextActionLabel,
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

function statusBadgeClass(status: ExamRequestStatus): string {
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

type ResultGroup = {
  examName: string;
  category: string;
  parameters: ResultParameterResponse[];
};

function groupResultsByExam(
  request: ExamRequestResponse,
  catalog: ExamTypeResponse[],
  parameters: ResultParameterResponse[],
): ResultGroup[] {
  const assigned = new Set<number>();
  const groups: ResultGroup[] = [];

  for (const exam of request.examTypes) {
    const full = catalog.find((t) => t.id === exam.id);
    const paramNames = new Set(parseExamParameterNames(full?.parameters));
    if (paramNames.size === 0) paramNames.add(exam.name);

    const matched = parameters.filter(
      (p) =>
        !assigned.has(p.id) &&
        (paramNames.has(p.parameterName) ||
          p.parameterName.toLowerCase().includes(exam.name.toLowerCase())),
    );
    for (const p of matched) assigned.add(p.id);

    if (matched.length > 0) {
      groups.push({
        examName: exam.name,
        category: examCategoryLabels[exam.category],
        parameters: matched,
      });
    }
  }

  const remaining = parameters.filter((p) => !assigned.has(p.id));
  if (remaining.length > 0) {
    groups.push({ examName: 'Autres paramètres', category: '', parameters: remaining });
  }

  if (groups.length === 0 && parameters.length > 0) {
    return [{ examName: 'Résultats', category: '', parameters }];
  }

  return groups;
}

function LabResultTable({ parameters }: { parameters: ResultParameterResponse[] }) {
  return (
    <ScrollTableRegion>
      <table className="data-table lab-result-table">
        <thead>
          <tr>
            <th>Paramètre</th>
            <th>Résultat</th>
            <th>Unité</th>
            <th>Valeurs de référence</th>
            <th>Interprétation</th>
          </tr>
        </thead>
        <tbody>
          {parameters.map((param) => (
            <tr key={param.id}>
              <td>{param.parameterName}</td>
              <td className={param.abnormal ? 'data-table__cell--abnormal' : undefined}>
                {param.value}
              </td>
              <td>{param.unit ?? '—'}</td>
              <td>
                {param.referenceMin || param.referenceMax
                  ? `${param.referenceMin ?? '—'} – ${param.referenceMax ?? '—'}`
                  : '—'}
              </td>
              <td>
                {param.abnormal ? (
                  <span className="lab-abnormal-flag">Anormal</span>
                ) : (
                  <span className="hint">Normal</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </ScrollTableRegion>
  );
}

function LabResultReport({
  result,
  request,
  examTypesCatalog,
}: {
  result: ExamResultResponse;
  request: ExamRequestResponse;
  examTypesCatalog: ExamTypeResponse[];
}) {
  const groups = useMemo(
    () => groupResultsByExam(request, examTypesCatalog, result.parameters),
    [request, examTypesCatalog, result.parameters],
  );

  return (
    <section className="card form-stack lab-result-report">
      <div className="lab-result-report__header">
        <h2 className="card__title" style={{ margin: 0 }}>
          Compte rendu laboratoire
        </h2>
        <p className="lab-result-report__meta">Publié le {formatLabInstant(result.resultedAt)}</p>
      </div>

      {result.annotation?.trim() && (
        <p className="lab-result-report__annotation">
          <strong>Annotation du laborantin :</strong> {result.annotation}
        </p>
      )}

      {groups.map((group) => (
        <div key={group.examName} className="lab-result-group">
          <h3 className="lab-result-group__title">
            {group.examName}
            {group.category ? <span className="lab-result-group__category">({group.category})</span> : null}
          </h3>
          <LabResultTable parameters={group.parameters} />
        </div>
      ))}
    </section>
  );
}

function LabRequestInfo({
  request,
  patientName,
}: {
  request: ExamRequestResponse;
  patientName: string;
}) {
  return (
    <section className="card form-stack lab-request-info">
      <h2 className="card__title">Détails de la demande</h2>
      <dl className="detail-grid">
        <div>
          <dt>Patient</dt>
          <dd>{patientName}</dd>
        </div>
        <div>
          <dt>Urgence</dt>
          <dd>
            {request.urgency === 'URGENT' ? (
              <span className="lab-urgency-pill lab-urgency-pill--urgent">{examUrgencyLabels[request.urgency]}</span>
            ) : (
              examUrgencyLabels[request.urgency]
            )}
          </dd>
        </div>
        <div>
          <dt>Statut</dt>
          <dd>
            <span className={statusBadgeClass(request.status)}>{examRequestStatusLabels[request.status]}</span>
          </dd>
        </div>
        <div>
          <dt>Demandé le</dt>
          <dd>{formatLabInstant(request.requestedAt)}</dd>
        </div>
      </dl>

      <div className="lab-request-info__exams">
        <span className="lab-request-info__exams-label">Examens demandés</span>
        <div className="lab-exam-chips">
          {request.examTypes.map((type) => (
            <span key={type.id} className="lab-exam-chip">
              {type.name}
              <span className="lab-exam-chip__category">({examCategoryLabels[type.category]})</span>
            </span>
          ))}
        </div>
      </div>

      {request.comment?.trim() && (
        <div className="lab-request-comment">
          <span className="lab-request-comment__label">Commentaire du prescripteur</span>
          <p className="lab-request-comment__text">{request.comment}</p>
        </div>
      )}
    </section>
  );
}

export function LabRequestDetailPage() {
  const { id } = useParams();
  const requestId = Number(id);
  const { user } = useAuth();
  const isLaborantin = isLabPortalUser(user);
  const isMedecin = hasRole(user, 'ROLE_MEDECIN');
  const canProcess =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_LABORANTIN');

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

  const [patientHistory, setPatientHistory] = useState<ExamRequestResponse[]>([]);
  const [showPostponeForm, setShowPostponeForm] = useState(false);
  const [postponeReason, setPostponeReason] = useState('');
  const [workflowSubmitting, setWorkflowSubmitting] = useState(false);
  const [workflowError, setWorkflowError] = useState<string | null>(null);

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

      // M3 — historique des analyses du patient (hors demande courante).
      try {
        const historyRes = await api.get<PageExamRequestResponse>('/api/v1/lab/exam-requests', {
          params: { patientId: requestRes.data.patientId, page: 0, size: 50 },
        });
        setPatientHistory(historyRes.data.content.filter((r) => r.id !== requestRes.data.id));
      } catch {
        setPatientHistory([]);
      }

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

  const canPostpone =
    canProcess && (request?.status === 'PENDING' || request?.status === 'SPECIMEN_COLLECTED');
  const canReactivate = request?.status === 'POSTPONED' && (isMedecin || canProcess);

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

  async function submitPostpone(e: React.FormEvent) {
    e.preventDefault();
    if (!request) return;
    setWorkflowSubmitting(true);
    setWorkflowError(null);
    try {
      await api.post<ExamRequestResponse>(`/api/v1/lab/exam-requests/${request.id}/postpone`, {
        reason: postponeReason.trim() || null,
      });
      setShowPostponeForm(false);
      setPostponeReason('');
      await loadAll();
    } catch (err) {
      setWorkflowError(getApiErrorMessage(err, 'Impossible de reporter la demande.'));
    } finally {
      setWorkflowSubmitting(false);
    }
  }

  async function reactivateRequest() {
    if (!request) return;
    setWorkflowSubmitting(true);
    setWorkflowError(null);
    try {
      await api.post<ExamRequestResponse>(`/api/v1/lab/exam-requests/${request.id}/reactivate`, {});
      await loadAll();
    } catch (err) {
      setWorkflowError(getApiErrorMessage(err, 'Impossible de réactiver la demande.'));
    } finally {
      setWorkflowSubmitting(false);
    }
  }

  if (loading) {
    return <LoadingBlock label="Chargement de la demande…" />;
  }

  if (error || !request) {
    return (
      <div className="page-stack">
        <PageHeader title="Demande d'examen" subtitle="Laboratoire" />
        <div className="error-banner">{error ?? 'Demande introuvable.'}</div>
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
        <Link
          to={isMedecin ? '/lab/requests?mine=1' : '/lab/requests?status=PENDING'}
          className="btn btn-ghost"
        >
          Liste des demandes
        </Link>
      </PageHeader>

      {(isLaborantin || canProcess || isMedecin) && <LabWorkflowSteps current={request.status} />}

      {isMedecin && !isLaborantin && (
        <>
          {request.status === 'RESULTS_AVAILABLE' && (
            <p className="lab-workflow-banner lab-workflow-banner--notify">
              <strong>Résultats publiés</strong> — consultez le compte rendu ci-dessous et intégrez-le au
              dossier du patient.
            </p>
          )}
          {request.status === 'PENDING' && (
            <p className="lab-workflow-banner">
              Demande transmise au laboratoire — prélèvement en attente.
            </p>
          )}
          {request.status === 'SPECIMEN_COLLECTED' && (
            <p className="lab-workflow-banner">
              Prélèvement effectué — analyse en cours, résultats attendus du laborantin.
            </p>
          )}
        </>
      )}

      {isLaborantin && request.status !== 'POSTPONED' && (
        <p className="lab-workflow-banner">
          <strong>Action :</strong> {labNextActionLabel(request.status)}
        </p>
      )}

      {request.status === 'POSTPONED' && request.postponeReason?.trim() && (
        <p className="lab-workflow-banner lab-workflow-banner--muted">
          <strong>Motif du report :</strong> {request.postponeReason}
        </p>
      )}

      {(canPostpone || canReactivate) && (
        <section className="card form-stack">
          <div className="lab-workflow-actions">
            {canReactivate && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={workflowSubmitting}
                onClick={() => void reactivateRequest()}
              >
                {workflowSubmitting ? 'Réactivation…' : 'Réactiver la demande'}
              </button>
            )}
            {canPostpone && !showPostponeForm && (
              <button
                type="button"
                className="btn btn-ghost"
                disabled={workflowSubmitting}
                onClick={() => setShowPostponeForm(true)}
              >
                Reporter la demande
              </button>
            )}
          </div>
          {canPostpone && showPostponeForm && (
            <form onSubmit={(e) => void submitPostpone(e)} className="form-stack">
              <label htmlFor="postpone-reason">Motif du report (optionnel)</label>
              <textarea
                id="postpone-reason"
                rows={2}
                value={postponeReason}
                onChange={(e) => setPostponeReason(e.target.value)}
                placeholder="Ex. échantillon insuffisant, réactif indisponible…"
              />
              <div className="lab-workflow-actions">
                <button type="submit" className="btn btn-primary" disabled={workflowSubmitting}>
                  {workflowSubmitting ? 'Report…' : 'Confirmer le report'}
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  disabled={workflowSubmitting}
                  onClick={() => {
                    setShowPostponeForm(false);
                    setPostponeReason('');
                  }}
                >
                  Annuler
                </button>
              </div>
            </form>
          )}
          {workflowError && <p className="form-error">{workflowError}</p>}
        </section>
      )}

      {request.status === 'RESULTS_AVAILABLE' && result && (
        <LabResultReport result={result} request={request} examTypesCatalog={examTypesCatalog} />
      )}

      <LabRequestInfo request={request} patientName={patientName} />

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

      <section className="card form-stack">
        <h2 className="card__title">Historique des analyses du patient</h2>
        {patientHistory.length === 0 ? (
          <p className="hint">Aucune autre demande d&apos;examen pour ce patient.</p>
        ) : (
          <ul className="lab-history-list">
            {patientHistory.map((item) => (
              <li key={item.id} className="lab-history-list__item">
                <Link to={`/lab/requests/${item.id}`} className="lab-history-list__link">
                  <span className="lab-history-list__date">{formatLabInstant(item.requestedAt)}</span>
                  <span className="lab-history-list__exams">
                    {examTypesSummary(item.examTypes.map((t) => t.name))}
                  </span>
                  <span className={statusBadgeClass(item.status)}>
                    {examRequestStatusLabels[item.status]}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
