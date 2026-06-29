import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ExamRequestStatus, ExamUrgency, PageExamRequestResponse } from '../api/types';

export type LabQueueStats = {
  pending: number | null;
  urgentPending: number | null;
  awaitingResults: number | null;
  resultsAvailable: number | null;
};

const emptyStats: LabQueueStats = {
  pending: null,
  urgentPending: null,
  awaitingResults: null,
  resultsAvailable: null,
};

async function countByStatus(status: ExamRequestStatus, urgency?: ExamUrgency): Promise<number> {
  const { data } = await api.get<PageExamRequestResponse>('/api/v1/lab/exam-requests', {
    params: { status, ...(urgency ? { urgency } : {}), page: 0, size: 1 },
  });
  return data.totalElements;
}

// Comptage exact côté serveur (M7) : plus de filtrage approximatif sur la première page.
async function countUrgentPending(): Promise<number> {
  return countByStatus('PENDING', 'URGENT');
}

/** Compteurs file laboratoire (en attente, urgentes, résultats à saisir). */
export function useLabQueueStats(enabled: boolean): { stats: LabQueueStats; loading: boolean } {
  const [stats, setStats] = useState<LabQueueStats>(emptyStats);
  const [loading, setLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    void (async () => {
      try {
        const [pending, awaitingResults, resultsAvailable, urgentPending] = await Promise.all([
          countByStatus('PENDING'),
          countByStatus('SPECIMEN_COLLECTED'),
          countByStatus('RESULTS_AVAILABLE'),
          countUrgentPending(),
        ]);
        if (!cancelled) {
          setStats({ pending, urgentPending, awaitingResults, resultsAvailable });
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
  }, [enabled]);

  return { stats, loading };
}
