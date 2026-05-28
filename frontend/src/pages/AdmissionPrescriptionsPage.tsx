import { useEffect, useState } from 'react';
import { Link, useOutletContext, useParams } from 'react-router-dom';
import type { AdmissionStayOutletContext } from '../admission/admissionStayContext';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { formatAdmissionPrescriptionDetails } from '../utils/prescriptionDisplay';
import { ADMISSION_CLOSED_MESSAGE, isAdmissionClosed } from '../utils/admissionStatus';
import type {
  PrescriptionLineCreateRequest,
  PrescriptionLineResponse,
  PrescriptionLineUpdateRequest,
} from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

type FormState = {
  medicationName: string;
  prescriptionDetails: string;
};

const emptyForm: FormState = {
  medicationName: '',
  prescriptionDetails: '',
};

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function toCreatePayload(
  form: FormState,
  prescriberName: string,
): PrescriptionLineCreateRequest {
  return {
    medicationName: form.medicationName.trim(),
    dosageText: form.prescriptionDetails.trim() || undefined,
    prescriberName: prescriberName.trim() || undefined,
    startDate: todayIsoDate(),
  };
}

export function AdmissionPrescriptionsPage() {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const stayContext = useOutletContext<AdmissionStayOutletContext | undefined>();
  const admission = stayContext?.admission ?? null;
  const admissionId = Number(id);
  const stayClosed = isAdmissionClosed(admission?.status);
  const connectedCaregiverName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';

  const [items, setItems] = useState<PrescriptionLineResponse[]>([]);
  const [createForm, setCreateForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<FormState>(emptyForm);
  const [editActive, setEditActive] = useState(true);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadItems();
  }, [admissionId]);

  useEffect(() => {
    if (stayClosed) {
      setEditingId(null);
      setEditForm(emptyForm);
      setEditActive(true);
    }
  }, [stayClosed]);

  async function loadItems() {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<PrescriptionLineResponse[]>(
        `/api/v1/admissions/${admissionId}/prescription-lines`,
      );
      setItems(data);
    } catch {
      setError('Impossible de charger les prescriptions.');
    } finally {
      setLoading(false);
    }
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!createForm.medicationName.trim() || !createForm.prescriptionDetails.trim()) {
      setError('Médicament et détails de la prescription sont obligatoires.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await api.post<PrescriptionLineResponse>(
        `/api/v1/admissions/${admissionId}/prescription-lines`,
        toCreatePayload(createForm, connectedCaregiverName),
      );
      setCreateForm(emptyForm);
      await loadItems();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter la prescription."));
    } finally {
      setSubmitting(false);
    }
  }

  function startEdit(item: PrescriptionLineResponse) {
    setEditingId(item.id);
    setEditForm({
      medicationName: item.medicationName,
      prescriptionDetails: formatAdmissionPrescriptionDetails(item),
    });
    setEditActive(item.active);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditForm(emptyForm);
    setEditActive(true);
  }

  async function onEditSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editingId == null) {
      return;
    }
    if (!editForm.medicationName.trim() || !editForm.prescriptionDetails.trim()) {
      setError('Médicament et détails de la prescription sont obligatoires.');
      return;
    }

    const payload: PrescriptionLineUpdateRequest = {
      ...toCreatePayload(editForm, connectedCaregiverName),
      active: editActive,
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.put<PrescriptionLineResponse>(
        `/api/v1/admissions/${admissionId}/prescription-lines/${editingId}`,
        payload,
      );
      cancelEdit();
      await loadItems();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre à jour la prescription.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      {!loading && stayClosed ? (
        <div
          className="card"
          style={{
            marginBottom: '1rem',
            borderColor: 'rgba(232, 93, 106, 0.45)',
            background: 'rgba(232, 93, 106, 0.06)',
          }}
        >
          <strong>Séjour clôturé</strong>
          <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)' }}>{ADMISSION_CLOSED_MESSAGE}</p>
        </div>
      ) : null}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span>Ajouter une prescription</span>
          <span style={{ color: 'var(--text)', fontWeight: 500 }}>{connectedCaregiverName}</span>
        </h3>
        <form
          onSubmit={onCreateSubmit}
          style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: '1fr 2fr auto', alignItems: 'end' }}
        >
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="medication-name">Médicament *</label>
            <input
              id="medication-name"
              value={createForm.medicationName}
              onChange={(e) => setCreateForm((v) => ({ ...v, medicationName: e.target.value }))}
              placeholder="Ex. Ceftriaxone"
              required
              disabled={stayClosed}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="prescription-details">Détails de la prescription *</label>
            <textarea
              id="prescription-details"
              rows={2}
              value={createForm.prescriptionDetails}
              onChange={(e) => setCreateForm((v) => ({ ...v, prescriptionDetails: e.target.value }))}
              placeholder="Ex. 1 g, 2 fois par jour pendant 7 jours"
              required
              disabled={stayClosed}
            />
          </div>
          <div>
            <button type="submit" className="btn btn-primary" disabled={submitting || stayClosed}>
              {submitting ? 'Enregistrement…' : 'Ajouter'}
            </button>
          </div>
        </form>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Médicament</th>
                  <th>Détails</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ color: 'var(--muted)' }}>
                      Aucune prescription pour cette admission.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.medicationName}</td>
                      <td>{formatAdmissionPrescriptionDetails(item)}</td>
                      <td>{item.active ? 'Active' : 'Inactive'}</td>
                      <td>
                        {stayClosed ? (
                          '—'
                        ) : (
                          <button type="button" className="btn btn-ghost btn-sm" onClick={() => startEdit(item)}>
                            Modifier
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={items.length}
            displayedCount={items.length}
            itemLabelPlural="prescription(s)"
          />
        </div>
      )}

      {editingId != null && !stayClosed && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <h3 style={{ marginTop: 0 }}>Modifier la prescription</h3>
          <form onSubmit={onEditSubmit} style={{ display: 'grid', gap: '0.75rem' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-medication-name">Médicament *</label>
              <input
                id="edit-medication-name"
                value={editForm.medicationName}
                onChange={(e) => setEditForm((v) => ({ ...v, medicationName: e.target.value }))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-prescription-details">Détails de la prescription *</label>
              <textarea
                id="edit-prescription-details"
                rows={3}
                value={editForm.prescriptionDetails}
                onChange={(e) => setEditForm((v) => ({ ...v, prescriptionDetails: e.target.value }))}
                required
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={editActive}
                onChange={(e) => setEditActive(e.target.checked)}
              />
              Prescription active
            </label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Enregistrement…' : 'Enregistrer'}
              </button>
              <button type="button" className="btn btn-ghost" onClick={cancelEdit}>
                Annuler
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
