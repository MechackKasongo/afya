import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { ExamCategory, ExamTypeCreateRequest, ExamTypeResponse } from '../api/types';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { AdminPageHeader } from '../components/admin/AdminPageHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { Toast } from '../components/ui/Toast';
import {
  examCategoryLabels,
  formatLabInstant,
  parseExamParameterNames,
} from '../utils/labDisplay';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';

type ExamTypeSortKey = 'id' | 'name' | 'category' | 'createdAt';

const categoryOptions: { value: ExamCategory; label: string }[] = (
  Object.entries(examCategoryLabels) as [ExamCategory, string][]
).map(([value, label]) => ({ value, label }));

export function LabExamTypesPage() {
  const [examTypes, setExamTypes] = useState<ExamTypeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<ExamTypeSortKey>('name');
  const [sortDir, setSortDir] = useState<TableSortDir>('asc');

  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createCategory, setCreateCategory] = useState<ExamCategory>('BIOLOGY');
  const [createDescription, setCreateDescription] = useState('');
  const [createParameters, setCreateParameters] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadExamTypes = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<ExamTypeResponse[]>('/api/v1/lab/exam-types');
      setExamTypes(data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les types d\'examen.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadExamTypes();
  }, [loadExamTypes]);

  const sortedRows = useMemo(() => {
    const rows = [...examTypes];
    rows.sort((a, b) => {
      let cmp = 0;
      switch (sortBy) {
        case 'id':
          cmp = compareNumbers(a.id, b.id, sortDir);
          break;
        case 'name':
          cmp = compareStrings(a.name, b.name, sortDir);
          break;
        case 'category':
          cmp = compareStrings(examCategoryLabels[a.category], examCategoryLabels[b.category], sortDir);
          break;
        case 'createdAt':
          cmp = compareStrings(a.createdAt, b.createdAt, sortDir);
          break;
      }
      return cmp;
    });
    return rows;
  }, [examTypes, sortBy, sortDir]);

  function handleSort(column: ExamTypeSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  function openCreateDrawer() {
    setCreateName('');
    setCreateCategory('BIOLOGY');
    setCreateDescription('');
    setCreateParameters('');
    setCreateError(null);
    setShowCreateDrawer(true);
  }

  async function submitCreate(e: React.FormEvent) {
    e.preventDefault();
    const name = createName.trim();
    if (!name) {
      setCreateError('Le nom est obligatoire.');
      return;
    }
    setSubmitting(true);
    setCreateError(null);
    const payload: ExamTypeCreateRequest = {
      name,
      category: createCategory,
      description: createDescription.trim() || null,
      parameters: createParameters.trim() || null,
    };
    try {
      await api.post<ExamTypeResponse>('/api/v1/lab/exam-types', payload);
      setShowCreateDrawer(false);
      setMessage(`Type « ${name} » créé.`);
      await loadExamTypes();
    } catch (err) {
      setCreateError(getApiErrorMessage(err, 'Impossible de créer le type d\'examen.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <AdminPageHeader
        title="Types d'examens"
        subtitle="catalogue laboratoire (référentiel administratif)"
      >
        <button type="button" className="btn btn-primary" onClick={openCreateDrawer}>
          Nouveau type
        </button>
      </AdminPageHeader>

      {message && <Toast message={message} onDismiss={() => setMessage(null)} />}
      {error && <p className="form-error">{error}</p>}
      {loading && <LoadingBlock label="Chargement du catalogue…" />}

      {!loading && (
        <ScrollTableRegion>
          <table className="data-table">
            <thead>
              <tr>
                <th className="data-table-col--id">
                  <DataTableColumnHeader
                    label="N°"
                    sortable
                    sortActive={sortBy === 'id'}
                    sortDir={sortDir}
                    onSort={() => handleSort('id')}
                  />
                </th>
                <th>
                  <DataTableColumnHeader
                    label="Nom"
                    sortable
                    sortActive={sortBy === 'name'}
                    sortDir={sortDir}
                    onSort={() => handleSort('name')}
                  />
                </th>
                <th>
                  <DataTableColumnHeader
                    label="Catégorie"
                    sortable
                    sortActive={sortBy === 'category'}
                    sortDir={sortDir}
                    onSort={() => handleSort('category')}
                  />
                </th>
                <th>Description</th>
                <th>Paramètres</th>
                <th>Actif</th>
                <th>
                  <DataTableColumnHeader
                    label="Créé le"
                    sortable
                    sortActive={sortBy === 'createdAt'}
                    sortDir={sortDir}
                    onSort={() => handleSort('createdAt')}
                  />
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedRows.length === 0 ? (
                <tr>
                  <td colSpan={7} className="empty-cell">
                    Aucun type d&apos;examen — créez le premier via « Nouveau type ».
                  </td>
                </tr>
              ) : (
                sortedRows.map((type) => {
                  const paramNames = parseExamParameterNames(type.parameters);
                  return (
                    <tr key={type.id}>
                      <td>{type.id}</td>
                      <td>{type.name}</td>
                      <td>{examCategoryLabels[type.category]}</td>
                      <td>{type.description?.trim() || '—'}</td>
                      <td>{paramNames.length > 0 ? paramNames.join(', ') : '—'}</td>
                      <td>{type.active ? 'Oui' : 'Non'}</td>
                      <td>{formatLabInstant(type.createdAt)}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
          <TableResultFooter
            totalElements={sortedRows.length}
            displayedCount={sortedRows.length}
            itemLabelPlural="type(s)"
          />
        </ScrollTableRegion>
      )}

      {showCreateDrawer && (
        <div className="drawer-backdrop" role="presentation" onClick={() => setShowCreateDrawer(false)}>
          <aside
            className="drawer-panel"
            role="dialog"
            aria-labelledby="exam-type-create-title"
            onClick={(e) => e.stopPropagation()}
          >
            <header className="drawer-panel__header">
              <h2 id="exam-type-create-title">Nouveau type d&apos;examen</h2>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowCreateDrawer(false)}>
                Fermer
              </button>
            </header>
            <form className="drawer-panel__body form-stack" onSubmit={(e) => void submitCreate(e)}>
              <label htmlFor="exam-type-name">Nom *</label>
              <input
                id="exam-type-name"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
                placeholder="Ex. NFS, Glycémie, Radio thorax"
                autoComplete="off"
                required
              />

              <label htmlFor="exam-type-category">Catégorie *</label>
              <select
                id="exam-type-category"
                value={createCategory}
                onChange={(e) => setCreateCategory(e.target.value as ExamCategory)}
              >
                {categoryOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <label htmlFor="exam-type-description">Description</label>
              <textarea
                id="exam-type-description"
                rows={2}
                value={createDescription}
                onChange={(e) => setCreateDescription(e.target.value)}
                placeholder="Indication clinique ou libellé long (optionnel)"
              />

              <label htmlFor="exam-type-parameters">Paramètres analysés</label>
              <textarea
                id="exam-type-parameters"
                rows={3}
                value={createParameters}
                onChange={(e) => setCreateParameters(e.target.value)}
                placeholder="Séparés par virgule ou point-virgule (ex. Hb, GB, Plaquettes)"
              />
              <p className="hint">Utilisés pour l&apos;affichage des résultats et la sélection en consultation.</p>

              {createError && <p className="form-error">{createError}</p>}

              <div className="drawer-panel__actions">
                <button type="button" className="btn btn-ghost" onClick={() => setShowCreateDrawer(false)}>
                  Annuler
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Enregistrement…' : 'Créer le type'}
                </button>
              </div>
            </form>
          </aside>
        </div>
      )}
    </div>
  );
}
