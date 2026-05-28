import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse, PatientCreateRequest, PatientResponse } from '../api/types';
import { Drawer } from '../components/ui/Drawer';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';
import { usePatientCareIndex } from '../hooks/usePatientCareIndex';
import {
  patientCareStatusBadgeClass,
  patientCareStatusLabel,
  resolvePatientCareStatus,
} from '../utils/patientCareStatus';

type PatientSortKey = 'id' | 'dossierNumber' | 'patient' | 'birthDate';

export function PatientsPage() {
  const [page, setPage] = useState<PagePatientResponse | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [filterInput, setFilterInput] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sortBy, setSortBy] = useState<PatientSortKey>('dossierNumber');
  const [sortDir, setSortDir] = useState<TableSortDir>('asc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [sex, setSex] = useState('M');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [postName, setPostName] = useState('');
  const [employer, setEmployer] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [profession, setProfession] = useState('');
  const [spouseName, setSpouseName] = useState('');
  const [spouseProfession, setSpouseProfession] = useState('');
  const { index: patientCareIndex } = usePatientCareIndex();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const backendSortBy =
      sortBy === 'patient' ? 'lastName' : sortBy === 'dossierNumber' ? 'dossierNumber' : sortBy;
    const params = new URLSearchParams({
      page: '0',
      size: String(LIST_FETCH_PAGE_SIZE),
      sortBy: backendSortBy,
      sortDir,
    });
    if (appliedQuery.trim()) params.set('query', appliedQuery.trim());
    api
      .get<PagePatientResponse>(`/api/v1/patients?${params}`)
      .then((res) => {
        if (!cancelled) setPage(res.data);
      })
      .catch(() => {
        if (!cancelled) setError('Impossible de charger les patients.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [appliedQuery, sortBy, sortDir, reloadKey]);

  function resetCreateForm() {
    setFirstName('');
    setLastName('');
    setBirthDate('');
    setSex('M');
    setPhone('');
    setEmail('');
    setAddress('');
    setPostName('');
    setEmployer('');
    setEmployeeId('');
    setProfession('');
    setSpouseName('');
    setSpouseProfession('');
    setCreateError(null);
  }

  async function onCreatePatientSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !birthDate || !sex.trim()) {
      setCreateError('Veuillez renseigner les champs obligatoires.');
      return;
    }

    const payload: PatientCreateRequest = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      birthDate,
      sex: sex.trim(),
      phone: phone.trim() || undefined,
      email: email.trim() || undefined,
      address: address.trim() || undefined,
      postName: postName.trim() || undefined,
      employer: employer.trim() || undefined,
      employeeId: employeeId.trim() || undefined,
      profession: profession.trim() || undefined,
      spouseName: spouseName.trim() || undefined,
      spouseProfession: spouseProfession.trim() || undefined,
    };

    setSubmittingCreate(true);
    setCreateError(null);
    try {
      await api.post<PatientResponse>('/api/v1/patients', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      setCreateError(getApiErrorMessage(err, 'Impossible de créer le patient.'));
    } finally {
      setSubmittingCreate(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedQuery(filterInput);
  }

  function onToggleSort(column: PatientSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  const sortedPatients = useMemo(() => {
    if (!page) return [];
    const rows = [...page.content];
    rows.sort((a, b) => {
      if (sortBy === 'id') return compareNumbers(a.id, b.id, sortDir);
      if (sortBy === 'dossierNumber') {
        return compareStrings(a.dossierNumber ?? '', b.dossierNumber ?? '', sortDir);
      }
      if (sortBy === 'birthDate') {
        return compareStrings(a.birthDate ?? '', b.birthDate ?? '', sortDir);
      }
      const aName = [a.firstName, a.lastName, a.postName].filter(Boolean).join(' ').trim();
      const bName = [b.firstName, b.lastName, b.postName].filter(Boolean).join(' ').trim();
      return compareStrings(aName, bName, sortDir);
    });
    return rows;
  }, [page, sortBy, sortDir]);

  return (
    <>
      <PageHeader title="Patients" />

      <div className="card filter-toolbar">
        <div className="filter-toolbar__inner">
          <form onSubmit={onSearchSubmit} className="filter-toolbar__form">
            <div className="field field--grow">
              <label htmlFor="patient-search">Recherche (nom, dossier…)</label>
              <input
                id="patient-search"
                value={filterInput}
                onChange={(e) => setFilterInput(e.target.value)}
                placeholder="Ex. Martin ou D2026"
              />
            </div>
            <button type="submit" className="btn btn-secondary">
              Rechercher
            </button>
          </form>

          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setShowCreateDrawer(true);
              setCreateError(null);
            }}
          >
            + Nouveau patient
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading && <LoadingBlock label="Chargement des patients…" />}

      {!loading && page && (
        <div className="card table-wrap">
          {page.content.length === 0 ? (
            <EmptyState
              title="Aucun patient trouvé"
              description="Modifiez les critères de recherche ou créez un nouveau dossier."
            />
          ) : (
            <>
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
                          label="Nom"
                          sortable
                          sortActive={sortBy === 'patient'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('patient')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Naissance"
                          sortable
                          sortActive={sortBy === 'birthDate'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('birthDate')}
                        />
                      </th>
                      <th>Sexe</th>
                      <th>
                        <DataTableColumnHeader
                          label="Dossier"
                          sortable
                          sortActive={sortBy === 'dossierNumber'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('dossierNumber')}
                        />
                      </th>
                      <th>État patient</th>
                      <th aria-label="Actions" />
                    </tr>
                  </thead>
                  <tbody>
                    {sortedPatients.map((p) => (
                      <tr key={p.id}>
                        <td className="data-table-col--id">{p.id}</td>
                        <td>{[p.firstName, p.lastName, p.postName].filter(Boolean).join(' ')}</td>
                        <td>{p.birthDate}</td>
                        <td>{p.sex}</td>
                        <td>
                          <span className="patient-dossier-number">{p.dossierNumber}</span>
                        </td>
                        <td>
                          {(() => {
                            const careStatus = resolvePatientCareStatus(p.id, p, patientCareIndex);
                            return (
                              <span className={patientCareStatusBadgeClass(careStatus)} title="État actuel du patient">
                                {patientCareStatusLabel(careStatus)}
                              </span>
                            );
                          })()}
                        </td>
                        <td>
                          <Link className="table-action-link" to={`/patients/${p.id}`}>
                            Ouvrir
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </ScrollTableRegion>
              <TableResultFooter
                totalElements={page.totalElements}
                displayedCount={page.content.length}
                itemLabelPlural="patient(s)"
              />
            </>
          )}
        </div>
      )}

      <Drawer
        open={showCreateDrawer}
        onClose={() => {
          setShowCreateDrawer(false);
          resetCreateForm();
        }}
        title="Créer un patient"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button type="submit" form="patient-create-form" className="btn btn-primary" disabled={submittingCreate}>
              {submittingCreate ? 'Création…' : 'Créer le patient'}
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setShowCreateDrawer(false);
                resetCreateForm();
              }}
            >
              Annuler
            </button>
          </div>
        }
      >
        {createError && <div className="error-banner">{createError}</div>}
        <form id="patient-create-form" onSubmit={onCreatePatientSubmit} className="card card--flat">
          <div className="form-grid">
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-firstName">Prénom *</label>
              <input id="drawer-firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-lastName">Nom *</label>
              <input id="drawer-lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-postName">Post-nom</label>
              <input id="drawer-postName" value={postName} onChange={(e) => setPostName(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-birthDate">Date de naissance *</label>
              <input id="drawer-birthDate" type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-sex">Sexe *</label>
              <select id="drawer-sex" value={sex} onChange={(e) => setSex(e.target.value)}>
                <option value="M">M</option>
                <option value="F">F</option>
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-phone">Téléphone</label>
              <input id="drawer-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-email">E-mail</label>
              <input id="drawer-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
              <label htmlFor="drawer-address">Adresse</label>
              <input id="drawer-address" value={address} onChange={(e) => setAddress(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-profession">Profession</label>
              <input id="drawer-profession" value={profession} onChange={(e) => setProfession(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-employer">Employeur</label>
              <input id="drawer-employer" value={employer} onChange={(e) => setEmployer(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-employeeId">Matricule</label>
              <input id="drawer-employeeId" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-spouseName">Nom du conjoint</label>
              <input id="drawer-spouseName" value={spouseName} onChange={(e) => setSpouseName(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-spouseProfession">Profession du conjoint</label>
              <input
                id="drawer-spouseProfession"
                value={spouseProfession}
                onChange={(e) => setSpouseProfession(e.target.value)}
              />
            </div>
          </div>
        </form>
      </Drawer>
    </>
  );
}
