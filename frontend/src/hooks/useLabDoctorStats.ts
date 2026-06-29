import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ExamRequestStatus, PageExamRequestResponse } from '../api/types';

export type LabDoctorStats = {
  resultsAvailable: number | null;
  pending: number | null;
  awaitingResults: number | null;
};

const emptyStats: LabDoctorStats = {
  resultsAvailable: null,
  pending: null,
  awaitingResults: null,
};

async function countForDoctor(doctorId: number, status: ExamRequestStatus): Promise<number> {
  const { data } = await api.get<PageExamRequestResponse>('/api/v1/lab/exam-requests', {
    params: { doctorId, status, page: 0, size: 1 },
  });
  return data.totalElements;
}

/** Suivi des demandes labo créées par le médecin connecté. */
export function useLabDoctorStats(doctorId: number | undefined, enabled: boolean) {
  const [stats, setStats] = useState<LabDoctorStats>(emptyStats);
  const [loading, setLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled || doctorId == null) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    void (async () => {
      try {
        const [resultsAvailable, pending, awaitingResults] = await Promise.all([
          countForDoctor(doctorId, 'RESULTS_AVAILABLE'),
          countForDoctor(doctorId, 'PENDING'),
          countForDoctor(doctorId, 'SPECIMEN_COLLECTED'),
        ]);
        if (!cancelled) {
          setStats({ resultsAvailable, pending, awaitingResults });
        }
      } catch {
        if (!cancelled) setStats(emptyStats);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [doctorId, enabled]);

  return { stats, loading };
}
