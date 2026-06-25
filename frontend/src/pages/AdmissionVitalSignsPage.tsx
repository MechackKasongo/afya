import { useEffect, useMemo, useState } from 'react';
import { useOutletContext, useParams } from 'react-router-dom';
import type { AdmissionStayOutletContext } from '../admission/admissionStayContext';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { VitalSignCreateRequest, VitalSignResponse, VitalSignSlot } from '../api/types';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { Drawer } from '../components/ui/Drawer';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { ADMISSION_CLOSED_MESSAGE, isAdmissionClosed } from '../utils/admissionStatus';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';

const slotLabels: Record<VitalSignSlot, string> = {
  MATIN: 'Matin',
  SOIR: 'Soir',
  JOURNEE: 'Journee',
};

type VitalSignSortKey = 'recordedAt' | 'slot' | 'ta' | 'pulse' | 'temperature' | 'weight' | 'diuresis';

function slotLabel(slot: VitalSignSlot | string | null | undefined): string {
  if (!slot || !(slot in slotLabels)) return '—';
  return slotLabels[slot as VitalSignSlot];
}

function slotSortLabel(slot: VitalSignSlot | null): string {
  if (!slot) return '';
  return slotLabels[slot] ?? '';
}

function toSortableNumber(value: number | string | null | undefined): number | null {
  if (value == null || value === '') return null;
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function compareNullableNumbers(
  a: number | string | null | undefined,
  b: number | string | null | undefined,
  dir: TableSortDir,
): number {
  const aNum = toSortableNumber(a);
  const bNum = toSortableNumber(b);
  if (aNum == null && bNum == null) return 0;
  if (aNum == null) return 1;
  if (bNum == null) return -1;
  return compareNumbers(aNum, bNum, dir);
}

function toNullableInt(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number.parseInt(trimmed, 10);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function nowIsoTimestamp(): string {
  return new Date().toISOString();
}

function formatRecordedAt(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('fr-FR');
}

function toNullableFloat(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number.parseFloat(trimmed);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function formatMetric(value: number | null | undefined, suffix = ''): string {
  if (value == null) return '—';
  return `${value}${suffix}`;
}

function formatBloodPressure(systolic: number | null | undefined, diastolic: number | null | undefined): string {
  if (systolic == null && diastolic == null) return '—';
  return `${systolic ?? '—'} / ${diastolic ?? '—'}`;
}

function hasStoolsNote(note: string | null | undefined): boolean {
  return Boolean(note?.trim());
}

function VitalSignDetail({ reading }: { reading: VitalSignResponse }) {
  return (
    <div className="vital-sign-detail">
      <p className="vital-sign-detail__meta">
        <strong>Date et heure</strong>
        {formatRecordedAt(reading.recordedAt)}
      </p>
      <p className="vital-sign-detail__meta">
        <strong>Créneau</strong>
        {reading.slot ? slotLabel(reading.slot) : 'Non précisé'}
      </p>
      <div className="vital-sign-detail__grid">
        <p className="vital-sign-detail__meta">
          <strong>TA</strong>
          {formatBloodPressure(reading.systolicBp, reading.diastolicBp)}
        </p>
        <p className="vital-sign-detail__meta">
          <strong>Pouls</strong>
          {formatMetric(reading.pulseBpm, ' bpm')}
        </p>
        <p className="vital-sign-detail__meta">
          <strong>Température</strong>
          {formatMetric(reading.temperatureCelsius, ' °C')}
        </p>
        <p className="vital-sign-detail__meta">
          <strong>Poids</strong>
          {formatMetric(reading.weightKg, ' kg')}
        </p>
        <p className="vital-sign-detail__meta">
          <strong>Diurèse</strong>
          {formatMetric(reading.diuresisMl, ' ml')}
        </p>
      </div>
      <div className="vital-sign-detail__body">
        <strong>Selles / observation</strong>
        <p>{reading.stoolsNote?.trim() || '—'}</p>
      </div>
    </div>
  );
}

export function AdmissionVitalSignsPage() {
  const { id } = useParams<{ id: string }>();
  const stayContext = useOutletContext<AdmissionStayOutletContext | undefined>();
  const admission = stayContext?.admission ?? null;
  const admissionId = Number(id);
  const stayClosed = isAdmissionClosed(admission?.status);

  const [items, setItems] = useState<VitalSignResponse[]>([]);
  const [sortBy, setSortBy] = useState<VitalSignSortKey>('recordedAt');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [slot, setSlot] = useState('');
  const [systolicBp, setSystolicBp] = useState('');
  const [diastolicBp, setDiastolicBp] = useState('');
  const [pulseBpm, setPulseBpm] = useState('');
  const [temperatureCelsius, setTemperatureCelsius] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [diuresisMl, setDiuresisMl] = useState('');
  const [stoolsNote, setStoolsNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedReading, setSelectedReading] = useState<VitalSignResponse | null>(null);

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadVitalSigns();
  }, [admissionId]);

  async function loadVitalSigns() {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<VitalSignResponse[]>(`/api/v1/admissions/${admissionId}/vital-signs`);
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      setItems([]);
      setError(
        getApiErrorMessage(
          err,
          'Impossible de charger les constantes. Vérifiez que admission-service (port 8084) et le BFF sont démarrés.',
        ),
      );
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();

    const payload: VitalSignCreateRequest = {
      recordedAt: nowIsoTimestamp(),
      systolicBp: toNullableInt(systolicBp),
      diastolicBp: toNullableInt(diastolicBp),
      pulseBpm: toNullableInt(pulseBpm),
      temperatureCelsius: toNullableFloat(temperatureCelsius),
      weightKg: toNullableFloat(weightKg),
      diuresisMl: toNullableInt(diuresisMl),
      stoolsNote: stoolsNote.trim() || undefined,
    };
    if (slot) payload.slot = slot as VitalSignSlot;

    setSubmitting(true);
    setError(null);
    try {
      await api.post<VitalSignResponse>(`/api/v1/admissions/${admissionId}/vital-signs`, payload);
      setSlot('');
      setSystolicBp('');
      setDiastolicBp('');
      setPulseBpm('');
      setTemperatureCelsius('');
      setWeightKg('');
      setDiuresisMl('');
      setStoolsNote('');
      await loadVitalSigns();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer la constante."));
    } finally {
      setSubmitting(false);
    }
  }

  function onToggleSort(column: VitalSignSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  const sortedItems = useMemo(() => {
    const rows = Array.isArray(items) ? [...items] : [];
    rows.sort((a, b) => {
      if (sortBy === 'recordedAt') {
        return compareStrings(a.recordedAt, b.recordedAt, sortDir);
      }
      if (sortBy === 'slot') {
        return compareStrings(slotSortLabel(a.slot), slotSortLabel(b.slot), sortDir);
      }
      if (sortBy === 'ta') {
        const systolic = compareNullableNumbers(a.systolicBp, b.systolicBp, sortDir);
        if (systolic !== 0) return systolic;
        return compareNullableNumbers(a.diastolicBp, b.diastolicBp, sortDir);
      }
      if (sortBy === 'pulse') {
        return compareNullableNumbers(a.pulseBpm, b.pulseBpm, sortDir);
      }
      if (sortBy === 'temperature') {
        return compareNullableNumbers(a.temperatureCelsius, b.temperatureCelsius, sortDir);
      }
      if (sortBy === 'weight') {
        return compareNullableNumbers(a.weightKg, b.weightKg, sortDir);
      }
      return compareNullableNumbers(a.diuresisMl, b.diuresisMl, sortDir);
    });
    return rows;
  }, [items, sortBy, sortDir]);

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
        <h3 style={{ marginTop: 0 }}>Ajouter un releve</h3>
        <form onSubmit={onSubmit} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="slot">Creneau</label>
            <select id="slot" value={slot} onChange={(e) => setSlot(e.target.value)} disabled={stayClosed}>
              <option value="">Non precise</option>
              <option value="MATIN">Matin</option>
              <option value="SOIR">Soir</option>
              <option value="JOURNEE">Journee</option>
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="systolic">TA systolique</label>
            <input id="systolic" inputMode="numeric" value={systolicBp} onChange={(e) => setSystolicBp(e.target.value)} placeholder="120" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="diastolic">TA diastolique</label>
            <input id="diastolic" inputMode="numeric" value={diastolicBp} onChange={(e) => setDiastolicBp(e.target.value)} placeholder="80" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="pulse">Pouls (bpm)</label>
            <input id="pulse" inputMode="numeric" value={pulseBpm} onChange={(e) => setPulseBpm(e.target.value)} placeholder="72" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="temp">Temperature (C)</label>
            <input id="temp" inputMode="decimal" value={temperatureCelsius} onChange={(e) => setTemperatureCelsius(e.target.value)} placeholder="37.2" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="weight">Poids (kg)</label>
            <input id="weight" inputMode="decimal" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} placeholder="68.5" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="diuresis">Diurese (ml)</label>
            <input id="diuresis" inputMode="numeric" value={diuresisMl} onChange={(e) => setDiuresisMl(e.target.value)} placeholder="500" disabled={stayClosed} />
          </div>
          <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
            <label htmlFor="stoolsNote">Selles / observation</label>
            <input id="stoolsNote" value={stoolsNote} onChange={(e) => setStoolsNote(e.target.value)} placeholder="Observation libre" disabled={stayClosed} />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting || stayClosed}>
              {submitting ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && (
        <div className="card table-wrap">
          <p className="vital-signs-hint">Cliquez sur une ligne pour afficher le détail du relevé.</p>
          <ScrollTableRegion>
            <table className="data-table vital-signs-table">
              <thead>
                <tr>
                  <th className="vital-signs-col--date">
                    <DataTableColumnHeader
                      label="Date/heure"
                      sortable
                      sortActive={sortBy === 'recordedAt'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('recordedAt')}
                    />
                  </th>
                  <th className="vital-signs-col--slot">
                    <DataTableColumnHeader
                      label="Creneau"
                      sortable
                      sortActive={sortBy === 'slot'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('slot')}
                    />
                  </th>
                  <th className="vital-signs-col--metric">
                    <DataTableColumnHeader
                      label="TA"
                      sortable
                      sortActive={sortBy === 'ta'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('ta')}
                    />
                  </th>
                  <th className="vital-signs-col--metric">
                    <DataTableColumnHeader
                      label="Pouls"
                      sortable
                      sortActive={sortBy === 'pulse'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('pulse')}
                    />
                  </th>
                  <th className="vital-signs-col--metric">
                    <DataTableColumnHeader
                      label="Temp."
                      sortable
                      sortActive={sortBy === 'temperature'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('temperature')}
                    />
                  </th>
                  <th className="vital-signs-col--metric">
                    <DataTableColumnHeader
                      label="Poids"
                      sortable
                      sortActive={sortBy === 'weight'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('weight')}
                    />
                  </th>
                  <th className="vital-signs-col--metric">
                    <DataTableColumnHeader
                      label="Diurese"
                      sortable
                      sortActive={sortBy === 'diuresis'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('diuresis')}
                    />
                  </th>
                  <th className="vital-signs-col--stools">Selles</th>
                </tr>
              </thead>
              <tbody>
                {sortedItems.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ color: 'var(--muted)' }}>
                      Aucun releve pour cette admission.
                    </td>
                  </tr>
                ) : (
                  sortedItems.map((item) => (
                    <tr
                      key={item.id}
                      className={`vital-signs-row${selectedReading?.id === item.id ? ' is-selected' : ''}`}
                      tabIndex={0}
                      role="button"
                      aria-label={`Détail du relevé du ${formatRecordedAt(item.recordedAt)}`}
                      onClick={() => setSelectedReading(item)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          setSelectedReading(item);
                        }
                      }}
                    >
                      <td className="vital-signs-table__date">{formatRecordedAt(item.recordedAt)}</td>
                      <td className="vital-signs-table__slot">{slotLabel(item.slot)}</td>
                      <td className="vital-signs-table__metric">
                        {item.systolicBp ?? '—'} / {item.diastolicBp ?? '—'}
                      </td>
                      <td className="vital-signs-table__metric">{item.pulseBpm ?? '—'}</td>
                      <td className="vital-signs-table__metric">{item.temperatureCelsius ?? '—'}</td>
                      <td className="vital-signs-table__metric">{item.weightKg ?? '—'}</td>
                      <td className="vital-signs-table__metric">{item.diuresisMl ?? '—'}</td>
                      <td className="vital-signs-col--stools">
                        {hasStoolsNote(item.stoolsNote) ? (
                          <span className="vital-signs-stools-badge" title="Observation enregistrée">
                            Note
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter totalElements={sortedItems.length} displayedCount={sortedItems.length} itemLabelPlural="relevé(s)" />
        </div>
      )}

      <Drawer
        open={selectedReading != null}
        onClose={() => setSelectedReading(null)}
        title={
          selectedReading
            ? `Relevé du ${formatRecordedAt(selectedReading.recordedAt)}`
            : 'Détail du relevé'
        }
        width="md"
      >
        {selectedReading ? <VitalSignDetail reading={selectedReading} /> : null}
      </Drawer>
    </>
  );
}
