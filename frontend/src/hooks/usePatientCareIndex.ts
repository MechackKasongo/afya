import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { PageAdmissionResponse, PageUrgenceResponse } from '../api/types';
import type { PatientCareIndex } from '../utils/patientCareStatus';

const URGENCE_OPEN_STATUSES = ['EN_COURS', 'EN_ATTENTE_TRIAGE'] as const;

export function usePatientCareIndex(enabled = true): {
  index: PatientCareIndex | null;
  loading: boolean;
} {
  const [index, setIndex] = useState<PatientCareIndex | null>(null);
  const [loading, setLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled) {
      setIndex(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    void (async () => {
      try {
        const [enCours, transfere, sortiVivant, ...urgencePages] = await Promise.all([
          api.get<PageAdmissionResponse>('/api/v1/admissions?status=EN_COURS&page=0&size=500'),
          api.get<PageAdmissionResponse>('/api/v1/admissions?status=TRANSFERE&page=0&size=500'),
          api.get<PageAdmissionResponse>('/api/v1/admissions?status=SORTI&page=0&size=500'),
          ...URGENCE_OPEN_STATUSES.map((status) =>
            api.get<PageUrgenceResponse>(`/api/v1/urgences?status=${status}&page=0&size=500`),
          ),
        ]);

        if (cancelled) return;

        const hospitalizedPatientIds = new Set<number>();
        for (const adm of [...enCours.data.content, ...transfere.data.content]) {
          hospitalizedPatientIds.add(adm.patientId);
        }

        const urgencesPatientIds = new Set<number>();
        for (const res of urgencePages) {
          for (const u of res.data.content) {
            urgencesPatientIds.add(u.patientId);
          }
        }

        const dischargedAlivePatientIds = new Set<number>();
        for (const adm of sortiVivant.data.content) {
          if (!hospitalizedPatientIds.has(adm.patientId)) {
            dischargedAlivePatientIds.add(adm.patientId);
          }
        }

        setIndex({ hospitalizedPatientIds, urgencesPatientIds, dischargedAlivePatientIds });
      } catch {
        if (!cancelled) {
          setIndex({
            hospitalizedPatientIds: new Set(),
            urgencesPatientIds: new Set(),
            dischargedAlivePatientIds: new Set(),
          });
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [enabled]);

  return { index, loading };
}
