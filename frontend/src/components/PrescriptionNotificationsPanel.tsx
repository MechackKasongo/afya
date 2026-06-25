import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PrescriptionNotificationResponse } from '../api/types';
import {
  formatPrescriptionNotificationDate,
  prescriptionNotificationStatusClass,
  prescriptionNotificationStatusLabel,
} from '../utils/prescriptionNotificationDisplay';
import {
  canLinkPrescriptionAdministrations,
  prescriptionAdministrationsPath,
} from '../utils/prescriptionRoutes';
import { ScrollTableRegion, TableResultFooter } from './ScrollTableRegion';

type PrescriptionNotificationsPanelProps = {
  patientId: number | null;
  admissionId?: number | null;
  prescriptionLineId?: number;
  autoMarkReadForLine?: boolean;
  title?: string;
};

export function PrescriptionNotificationsPanel({
  patientId,
  admissionId,
  prescriptionLineId,
  autoMarkReadForLine = false,
  title = 'Notifications prescriptions',
}: PrescriptionNotificationsPanelProps) {
  const [items, setItems] = useState<PrescriptionNotificationResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [markingId, setMarkingId] = useState<number | null>(null);

  const loadItems = useCallback(async () => {
    if (patientId == null || !Number.isFinite(patientId)) {
      setItems([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<PrescriptionNotificationResponse[]>(
        `/api/v1/patients/${patientId}/prescription-notifications`,
      );
      setItems(data);
    } catch (err) {
      setItems([]);
      setError(getApiErrorMessage(err, 'Impossible de charger les notifications prescriptions.'));
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => {
    void loadItems();
  }, [loadItems]);

  const displayedItems = useMemo(() => {
    if (prescriptionLineId == null || !Number.isFinite(prescriptionLineId)) {
      return items;
    }
    return items.filter((item) => item.prescriptionLineId === prescriptionLineId);
  }, [items, prescriptionLineId]);

  const markRead = useCallback(
    async (notificationId: number) => {
      if (patientId == null || !Number.isFinite(patientId)) {
        return;
      }
      setMarkingId(notificationId);
      setError(null);
      try {
        await api.patch<PrescriptionNotificationResponse>(
          `/api/v1/patients/${patientId}/prescription-notifications/${notificationId}/read`,
        );
        await loadItems();
      } catch (err) {
        setError(getApiErrorMessage(err, 'Impossible de marquer la notification comme lue.'));
      } finally {
        setMarkingId(null);
      }
    },
    [loadItems, patientId],
  );

  useEffect(() => {
    if (!autoMarkReadForLine || prescriptionLineId == null || loading) {
      return;
    }
    const pending = items.find(
      (item) => item.prescriptionLineId === prescriptionLineId && item.status === 'ENVOYEE',
    );
    if (pending) {
      void markRead(pending.id);
    }
  }, [autoMarkReadForLine, items, loading, markRead, prescriptionLineId]);

  if (patientId == null || !Number.isFinite(patientId)) {
    return null;
  }

  const pendingCount = displayedItems.filter((item) => item.status === 'ENVOYEE').length;

  return (
    <div className="card prescription-notifications-panel" style={{ marginBottom: '1rem' }}>
      <div className="prescription-notifications-panel__head">
        <h3 style={{ margin: 0 }}>{title}</h3>
        {pendingCount > 0 ? (
          <span className="prescription-notifications-panel__pending">{pendingCount} nouvelle(s)</span>
        ) : null}
      </div>

      {error ? <div className="error-banner">{error}</div> : null}
      {loading ? <p style={{ color: 'var(--muted)', marginBottom: 0 }}>Chargement des notifications…</p> : null}

      {!loading && (
        <ScrollTableRegion>
          <table className="data-table">
            <thead>
              <tr>
                <th>Médicament</th>
                <th>Reçue le</th>
                <th>Statut</th>
                <th>Infirmière</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {displayedItems.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ color: 'var(--muted)' }}>
                    Aucune notification prescription.
                  </td>
                </tr>
              ) : (
                displayedItems.map((item) => (
                  <tr
                    key={item.id}
                    className={
                      item.status === 'ENVOYEE' ? 'prescription-notifications-panel__row--pending' : undefined
                    }
                  >
                    <td>{item.drugName}</td>
                    <td>{formatPrescriptionNotificationDate(item.sentAt)}</td>
                    <td>
                      <span className={prescriptionNotificationStatusClass(item.status)}>
                        {prescriptionNotificationStatusLabel(item.status)}
                      </span>
                    </td>
                    <td>{item.nurseUsername ?? '—'}</td>
                    <td>
                      <div className="prescription-notifications-panel__actions">
                        {canLinkPrescriptionAdministrations(admissionId) ? (
                          <Link
                            to={prescriptionAdministrationsPath(admissionId, item.prescriptionLineId)}
                            className="btn btn-ghost btn-sm"
                          >
                            {item.status === 'EXECUTEE' ? 'Voir administrations' : 'Administrer'}
                          </Link>
                        ) : null}
                        {item.status === 'ENVOYEE' ? (
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            disabled={markingId === item.id}
                            onClick={() => void markRead(item.id)}
                          >
                            {markingId === item.id ? '…' : 'Marquer lue'}
                          </button>
                        ) : null}
                        {!canLinkPrescriptionAdministrations(admissionId) && item.status !== 'ENVOYEE'
                          ? '—'
                          : null}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </ScrollTableRegion>
      )}

      {!loading && displayedItems.length > 0 ? (
        <TableResultFooter
          totalElements={displayedItems.length}
          displayedCount={displayedItems.length}
          itemLabelPlural="notification(s)"
        />
      ) : null}
    </div>
  );
}
