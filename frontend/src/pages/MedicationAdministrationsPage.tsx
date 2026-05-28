import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import type {
  AdmissionResponse,
  MedicationAdministrationCreateRequest,
  MedicationAdministrationResponse,
  VitalSignSlot,
} from '../api/types';
import { ADMISSION_CLOSED_MESSAGE, isAdmissionClosed } from '../utils/admissionStatus';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

const slotLabels: Record<VitalSignSlot, string> = {
  MATIN: 'Matin',
  SOIR: 'Soir',
  JOURNEE: 'Journee',
};

export function MedicationAdministrationsPage() {
  const { user } = useAuth();
  const { admissionId, lineId } = useParams<{ admissionId: string; lineId: string }>();
  const numericAdmissionId = Number(admissionId);
  const numericLineId = Number(lineId);
  const connectedCaregiverName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';

  const [items, setItems] = useState<MedicationAdministrationResponse[]>([]);
  const [administrationDate, setAdministrationDate] = useState('');
  const [slot, setSlot] = useState<VitalSignSlot>('MATIN');
  const [administered, setAdministered] = useState(true);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const stayClosed = isAdmissionClosed(admission?.status);

  useEffect(() => {
    if (!Number.isFinite(numericAdmissionId) || !Number.isFinite(numericLineId)) {
      setError('Parametres admission/prescription invalides.');
      setLoading(false);
      return;
    }
    void loadItems();
  }, [numericAdmissionId, numericLineId]);

  async function loadItems() {
    setLoading(true);
    setError(null);
    try {
      const [{ data }, admissionRes] = await Promise.all([
        api.get<MedicationAdministrationResponse[]>(
          `/api/v1/admissions/${numericAdmissionId}/prescription-lines/${numericLineId}/administrations`,
        ),
        api.get<AdmissionResponse>(`/api/v1/admissions/${numericAdmissionId}`),
      ]);
      setItems(data);
      setAdmission(admissionRes.data);
    } catch {
      setError("Impossible de charger les administrations.");
      setAdmission(null);
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!administrationDate) {
      setError("La date d'administration est obligatoire.");
      return;
    }

    const payload: MedicationAdministrationCreateRequest = {
      administrationDate,
      slot,
      administered,
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.post<MedicationAdministrationResponse>(
        `/api/v1/admissions/${numericAdmissionId}/prescription-lines/${numericLineId}/administrations`,
        payload,
      );
      setAdministrationDate('');
      setSlot('MATIN');
      setAdministered(true);
      await loadItems();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer l'administration."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <Link to={`/admissions/${admissionId}/prescriptions`}>Retour aux prescriptions</Link>
        <span>
          admission {admissionId} · ligne {lineId}
        </span>
      </p>

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
          <span>Enregistrer une administration</span>
          <span style={{ color: 'var(--text)', fontWeight: 500 }}>{connectedCaregiverName}</span>
        </h3>
        <form onSubmit={onSubmit} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', alignItems: 'end' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="administration-date">Date *</label>
            <input
              id="administration-date"
              type="date"
              value={administrationDate}
              onChange={(e) => setAdministrationDate(e.target.value)}
              required
              disabled={stayClosed}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="administration-slot">Creneau *</label>
            <select
              id="administration-slot"
              value={slot}
              onChange={(e) => setSlot(e.target.value as VitalSignSlot)}
              disabled={stayClosed}
            >
              <option value="MATIN">Matin</option>
              <option value="SOIR">Soir</option>
              <option value="JOURNEE">Journee</option>
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="administration-status">Etat</label>
            <select
              id="administration-status"
              value={administered ? 'true' : 'false'}
              onChange={(e) => setAdministered(e.target.value === 'true')}
              disabled={stayClosed}
            >
              <option value="true">Administree</option>
              <option value="false">Non administree</option>
            </select>
          </div>
          <div>
            <button type="submit" className="btn btn-primary" disabled={submitting || stayClosed}>
              {submitting ? 'Enregistrement...' : 'Ajouter'}
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
                  <th>Date</th>
                  <th>Creneau</th>
                  <th>Etat</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={3} style={{ color: 'var(--muted)' }}>
                      Aucune administration enregistree.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.administrationDate}</td>
                      <td>{slotLabels[item.slot]}</td>
                      <td>{item.administered ? 'Administree' : 'Non administree'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter totalElements={items.length} displayedCount={items.length} itemLabelPlural="administration(s)" />
        </div>
      )}
    </>
  );
}
