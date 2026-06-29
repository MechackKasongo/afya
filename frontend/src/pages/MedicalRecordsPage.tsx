import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse } from '../api/types';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';

type SortKey = 'id' | 'patient' | 'dossierNumber';

export function MedicalRecordsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState<PagePatientResponse | null>(null);
  const [queryInput, setQueryInput] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sortBy, setSortBy] = useState<SortKey>('patient');
  const [sortDir, setSortDir] = useState<TableSortDir>('asc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadPatients();
  }, [appliedQuery, sortBy, sortDir]);

  async function loadPatients() {
    setLoading(true);
    setError(null);
    try {
      const backendSortBy = sortBy === 'patient' ? 'lastName' : sortBy;
      const params = new URLSearchParams({ page: '0', size: String(LIST_FETCH_PAGE_SIZE), sortBy: backendSortBy, sortDir });
      if (appliedQuery.trim()) params.set('query', appliedQuery.trim());
      const { data } = await api.get<PagePatientResponse>(`/api/v1/patients?${params}`);
      setPage(data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les dossiers médicaux.'));
    } finally {
      setLoading(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedQuery(queryInput);
  }

  function openRecord(patientId: number) {
    setError(null);
    navigate(`/medical-records/${patientId}`);
  }

  function onToggleSort(column: SortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  const sortedPatients = useMemo(() => {
    if (!page) return [];
    const patients = [...page.content];
    patients.sort((a, b) => {
      if (sortBy === 'id') {
        return compareNumbers(a.id, b.id, sortDir);
      }
      if (sortBy === 'dossierNumber') {
        return compareStrings(a.dossierNumber ?? '', b.dossierNumber ?? '', sortDir);
      }
      const aName = [a.firstName, a.lastName, a.postName].filter(Boolean).join(' ').trim();
      const bName = [b.firstName, b.lastName, b.postName].filter(Boolean).join(' ').trim();
      return compareStrings(aName, bName, sortDir);
    });
    return patients;
  }, [page, sortBy, sortDir]);

  return (
    <div className="page-stack">
      <PageHeader title="Dossiers médicaux" subtitle="Vue consolidée du patient : allergies, antécédents, notes et séjours" />

      {error && <div className="error-banner">{error}</div>}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <form onSubmit={onSearchSubmit} className="shared-search-form">
          <div className="field shared-search-field" style={{ marginBottom: 0 }}>
            <label htmlFor="medical-records-search">Rechercher patient</label>
            <input
              id="medical-records-search"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              placeholder="Nom, post-nom ou numéro dossier"
            />
          </div>
          <button type="submit" className="btn btn-primary">Rechercher</button>
        </form>
      </div>

      {loading && <LoadingBlock label="Chargement des dossiers médicaux…" />}
      {!loading && page && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th className="data-table-col--id">
                    <DataTableColumnHeader
                      label="ID"
                      sortable
                      sortActive={sortBy === 'id'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('id')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Patient"
                      sortable
                      sortActive={sortBy === 'patient'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('patient')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Dossier patient"
                      sortable
                      sortActive={sortBy === 'dossierNumber'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('dossierNumber')}
                    />
                  </th>
                  <th>Statut patient</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sortedPatients.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="empty-cell">Aucun patient trouvé.</td>
                  </tr>
                ) : (
                  sortedPatients.map((patient) => (
                    <tr
                      key={patient.id}
                      className="data-table-row--clickable"
                      role="link"
                      tabIndex={0}
                      aria-label={`Ouvrir le dossier médical du patient ${patient.id}`}
                      onClick={() => openRecord(patient.id)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          openRecord(patient.id);
                        }
                      }}
                    >
                      <td className="data-table-col--id">{patient.id}</td>
                      <td>
                        {[patient.firstName, patient.lastName, patient.postName].filter(Boolean).join(' ') || patient.id}
                      </td>
                      <td>
                        <span className="patient-dossier-number">{patient.dossierNumber}</span>
                      </td>
                      <td>
                        {patient.deceasedAt ? (
                          <span className="patient-status-badge patient-status-badge--deceased">Décès</span>
                        ) : (
                          <span className="patient-status-badge patient-status-badge--active">Actif</span>
                        )}
                      </td>
                      <td>
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            openRecord(patient.id);
                          }}
                          style={{
                            border: 'none',
                            background: 'none',
                            padding: 0,
                            color: 'var(--accent)',
                            cursor: 'pointer',
                            font: 'inherit',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                          }}
                        >
                          <span aria-hidden="true">↗</span>
                          Ouvrir dossier
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={page.totalElements}
            displayedCount={page.content.length}
            itemLabelPlural="patient(s)"
          />
        </div>
      )}
    </div>
  );
}
