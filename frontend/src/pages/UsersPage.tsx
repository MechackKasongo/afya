import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  HospitalServiceResponse,
  PageHospitalServiceResponse,
  PageUserResponse,
  PasswordPreviewResponse,
  RoleOptionResponse,
  UserCreateRequest,
  UserCredentialsResponse,
  UserResponse,
  UserUpdateRequest,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import {
  DataTableColumnHeader,
  DataTableFilterCell,
  DataTableFilterHint,
  DataTableFilterSelectCell,
} from '../components/DataTableColumnHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { Drawer } from '../components/ui/Drawer';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { Toast } from '../components/ui/Toast';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import { defaultSortDirForColumn, toggleTableSort, type TableSortDir } from '../utils/tableSort';
import {
  buildUserFullNamePreview,
  buildUserUsernamePreview,
  isValidEmail,
  roleLabelFor,
  serviceNamesFor,
  splitFullNameForPassword,
} from '../utils/users';

type UserSortKey = 'id' | 'username' | 'fullName' | 'role' | 'active';
type UserActiveFilter = 'all' | 'active' | 'inactive';
type FilterColumn = 'username' | 'role' | 'service' | 'active';

/** Valeur du filtre service : utilisateurs sans affectation hospitalière. */
const SERVICE_FILTER_NONE = '__none__';

type ColumnFilters = {
  username: string;
  role: string;
  service: string;
  active: UserActiveFilter;
};

const emptyFilters: ColumnFilters = {
  username: '',
  role: '',
  service: '',
  active: 'all',
};

function hasUserFilters(filters: ColumnFilters): boolean {
  return (
    filters.username.trim() !== '' ||
    filters.role !== '' ||
    filters.service !== '' ||
    filters.active !== 'all'
  );
}

function activeFilterToParam(filter: UserActiveFilter): boolean | undefined {
  if (filter === 'active') return true;
  if (filter === 'inactive') return false;
  return undefined;
}

export function UsersPage() {
  const { user: currentUser } = useAuth();
  const [page, setPage] = useState<PageUserResponse | null>(null);
  const [searchInput, setSearchInput] = useState('');
  const [openFilterCol, setOpenFilterCol] = useState<FilterColumn | null>(null);
  const [filterDraft, setFilterDraft] = useState<ColumnFilters>({ ...emptyFilters });
  const [appliedFilters, setAppliedFilters] = useState<ColumnFilters>({ ...emptyFilters });
  const [sortBy, setSortBy] = useState<UserSortKey>('id');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [roles, setRoles] = useState<RoleOptionResponse[]>([]);
  const [hospitalServicesCatalog, setHospitalServicesCatalog] = useState<HospitalServiceResponse[]>([]);
  const [createHospitalServiceIds, setCreateHospitalServiceIds] = useState<number[]>([]);
  const [createFirstName, setCreateFirstName] = useState('');
  const [createLastName, setCreateLastName] = useState('');
  const [createPostName, setCreatePostName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createRole, setCreateRole] = useState('ROLE_RECEPTION');
  const [createPasswordLength, setCreatePasswordLength] = useState<12 | 16>(16);
  const [passwordPreview, setPasswordPreview] = useState<string | null>(null);
  const [passwordVariation, setPasswordVariation] = useState(0);
  const [pwdSuggestLoading, setPwdSuggestLoading] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null);
  const [selectedCredentials, setSelectedCredentials] = useState<UserCredentialsResponse | null>(null);
  const [freshPassword, setFreshPassword] = useState<string | null>(null);
  const [credentialsLoading, setCredentialsLoading] = useState(false);
  const [editFullName, setEditFullName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editRole, setEditRole] = useState('ROLE_RECEPTION');
  const [editHospitalServiceIds, setEditHospitalServiceIds] = useState<number[]>([]);
  const [editPassword, setEditPassword] = useState('');
  const [editPasswordLength, setEditPasswordLength] = useState<12 | 16>(16);
  const [editPasswordPreview, setEditPasswordPreview] = useState<string | null>(null);
  const [editPasswordVariation, setEditPasswordVariation] = useState(0);
  const [editPwdSuggestLoading, setEditPwdSuggestLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [detailEditing, setDetailEditing] = useState(false);
  /** Évite qu’un double-clic sur « Modifier » déclenche « Enregistrer » (même emplacement). */
  const [detailSubmitReady, setDetailSubmitReady] = useState(false);
  const [credentialsError, setCredentialsError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    void loadRoles();
    void (async () => {
      try {
        const { data } = await api.get<PageHospitalServiceResponse>(
          '/api/v1/hospital-services?activeOnly=true&page=0&size=500'
        );
        setHospitalServicesCatalog(data.content);
      } catch {
        setHospitalServicesCatalog([]);
      }
    })();
  }, []);

  const loadUserCredentials = useCallback(async (userId: number) => {
    setCredentialsLoading(true);
    setCredentialsError(null);
    try {
      const { data } = await api.get<UserCredentialsResponse>(`/api/v1/users/${userId}/credentials`);
      setSelectedCredentials(data);
      return data;
    } catch (err: unknown) {
      setSelectedCredentials(null);
      setCredentialsError(
        getApiErrorMessage(
          err,
          'Impossible de charger le mot de passe depuis le journal. Vérifiez que le BFF est à jour (redémarrage après mise à jour).',
        ),
      );
      return null;
    } finally {
      setCredentialsLoading(false);
    }
  }, []);

  const fetchEditPasswordSuggestion = useCallback(
    async (variation: number) => {
      const { firstName, lastName } = splitFullNameForPassword(editFullName);
      if (!firstName || !lastName) {
        return;
      }
      setEditPwdSuggestLoading(true);
      setDetailError(null);
      try {
        const { data } = await api.post<PasswordPreviewResponse>('/api/v1/users/password-preview', {
          firstName,
          lastName,
          generatedPasswordLength: editPasswordLength,
          variation,
        });
        setEditPasswordPreview(data.password);
        setEditPasswordVariation(variation);
      } catch (err) {
        setDetailError(getApiErrorMessage(err, 'Impossible de proposer un mot de passe.'));
      } finally {
        setEditPwdSuggestLoading(false);
      }
    },
    [editFullName, editPasswordLength]
  );

  useEffect(() => {
    void loadUsers();
  }, [appliedFilters, sortBy, sortDir]);

  useEffect(() => {
    if (!selectedUser || detailEditing || freshPassword) {
      if (!selectedUser) {
        setSelectedCredentials(null);
        setFreshPassword(null);
        setCredentialsError(null);
      }
      return;
    }
    let cancelled = false;
    void loadUserCredentials(selectedUser.id).then(() => {
      if (cancelled) return;
    });
    return () => {
      cancelled = true;
    };
  }, [selectedUser?.id, detailEditing, freshPassword, loadUserCredentials]);

  useEffect(() => {
    if (!detailEditing) {
      setDetailSubmitReady(false);
      setEditPasswordPreview(null);
      setEditPasswordVariation(0);
      return;
    }
    setDetailSubmitReady(false);
    const readyTimer = window.setTimeout(() => setDetailSubmitReady(true), 400);
    const { firstName, lastName } = splitFullNameForPassword(editFullName);
    let suggestTimer: number | undefined;
    if (firstName && lastName) {
      suggestTimer = window.setTimeout(() => {
        void fetchEditPasswordSuggestion(0);
      }, 420);
    } else {
      setEditPasswordPreview(null);
      setEditPasswordVariation(0);
    }
    return () => {
      window.clearTimeout(readyTimer);
      if (suggestTimer !== undefined) window.clearTimeout(suggestTimer);
    };
  }, [detailEditing, editFullName, editPasswordLength, fetchEditPasswordSuggestion]);

  const hasActiveFilters = hasUserFilters(appliedFilters);
  const isSelf = selectedUser != null && currentUser?.username === selectedUser.username;
  const displayedPassword =
    freshPassword ?? selectedCredentials?.password ?? null;
  const showFilterRow = openFilterCol !== null;

  const filterHint = useMemo(() => {
    if (!hasActiveFilters) return null;
    const parts: string[] = [];
    if (appliedFilters.username.trim()) {
      parts.push(`Recherche « ${appliedFilters.username.trim()} »`);
    }
    if (appliedFilters.role) {
      parts.push(`Rôle ${roleLabelFor(roles, appliedFilters.role)}`);
    }
    if (appliedFilters.service === SERVICE_FILTER_NONE) {
      parts.push('Sans service affecté');
    } else if (appliedFilters.service) {
      const name = hospitalServicesCatalog.find((s) => String(s.id) === appliedFilters.service)?.name;
      parts.push(`Service ${name ?? appliedFilters.service}`);
    }
    if (appliedFilters.active === 'active') parts.push('Actifs');
    if (appliedFilters.active === 'inactive') parts.push('Inactifs');
    return parts.join(' · ');
  }, [appliedFilters, hasActiveFilters, roles, hospitalServicesCatalog]);

  const roleFilterOptions = useMemo(
    () => roles.map((r) => ({ value: r.name, label: r.label })),
    [roles],
  );

  const serviceFilterOptions = useMemo(
    () => [
      { value: SERVICE_FILTER_NONE, label: 'Sans service affecté' },
      ...hospitalServicesCatalog.map((s) => ({ value: String(s.id), label: s.name })),
    ],
    [hospitalServicesCatalog],
  );

  const activeFilterOptions = useMemo(
    () => [
      { value: 'active', label: 'Actifs' },
      { value: 'inactive', label: 'Inactifs' },
    ],
    [],
  );

  const suggestedFullName = useMemo(
    () => buildUserFullNamePreview(createFirstName, createLastName, createPostName),
    [createFirstName, createLastName, createPostName]
  );

  const suggestedUsername = useMemo(() => {
    const preview = buildUserUsernamePreview(suggestedFullName);
    return preview || '—';
  }, [suggestedFullName]);

  const fetchPasswordSuggestion = useCallback(
    async (variation: number) => {
      if (!createFirstName.trim() || !createLastName.trim()) {
        return;
      }
      setPwdSuggestLoading(true);
      setCreateError(null);
      try {
        const { data } = await api.post<PasswordPreviewResponse>('/api/v1/users/password-preview', {
          firstName: createFirstName.trim(),
          lastName: createLastName.trim(),
          postName: createPostName.trim() || undefined,
          generatedPasswordLength: createPasswordLength,
          variation,
        });
        setPasswordPreview(data.password);
        setPasswordVariation(variation);
      } catch (err) {
        setCreateError(getApiErrorMessage(err, 'Impossible de proposer un mot de passe.'));
      } finally {
        setPwdSuggestLoading(false);
      }
    },
    [createFirstName, createLastName, createPostName, createPasswordLength]
  );

  useEffect(() => {
    if (!createFirstName.trim() || !createLastName.trim()) {
      setPasswordPreview(null);
      setPasswordVariation(0);
      return;
    }
    const handle = window.setTimeout(() => {
      void fetchPasswordSuggestion(0);
    }, 420);
    return () => window.clearTimeout(handle);
  }, [createFirstName, createLastName, createPostName, createPasswordLength, fetchPasswordSuggestion]);

  async function loadRoles() {
    try {
      const { data } = await api.get<RoleOptionResponse[]>('/api/v1/users/roles');
      setRoles(data);
      if (data.length > 0) {
        setCreateRole(data[0].name);
      }
    } catch {
      setRoles([]);
    }
  }

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: '0',
        size: String(LIST_FETCH_PAGE_SIZE),
        sortBy,
        sortDir,
      });
      if (appliedFilters.username.trim()) params.set('query', appliedFilters.username.trim());
      if (appliedFilters.role) params.set('role', appliedFilters.role);
      const activeParam = activeFilterToParam(appliedFilters.active);
      if (activeParam !== undefined) params.set('active', String(activeParam));
      if (appliedFilters.service === SERVICE_FILTER_NONE) {
        params.set('withoutHospitalService', 'true');
      } else if (appliedFilters.service) {
        params.set('hospitalServiceId', appliedFilters.service);
      }
      const { data } = await api.get<PageUserResponse>(`/api/v1/users?${params.toString()}`);
      setPage(data);
      setSelectedUser((prev) => {
        if (!prev) return null;
        return data.content.find((u) => u.id === prev.id) ?? null;
      });
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les utilisateurs.'));
    } finally {
      setLoading(false);
    }
  }

  function resetCreateUserForm() {
    setCreateFirstName('');
    setCreateLastName('');
    setCreatePostName('');
    setCreateEmail('');
    setCreateRole(roles[0]?.name ?? 'ROLE_RECEPTION');
    setCreateHospitalServiceIds([]);
    setCreatePasswordLength(16);
    setPasswordPreview(null);
    setPasswordVariation(0);
    setCreateError(null);
  }

  function closeCreateDrawer() {
    setShowCreateDrawer(false);
    resetCreateUserForm();
  }

  async function onCreateUser(e: React.FormEvent) {
    e.preventDefault();
    if (!createFirstName.trim() || !createLastName.trim() || !createRole.trim()) {
      setCreateError('Prénom, nom et rôle sont requis.');
      return;
    }
    if (createEmail.trim() && !isValidEmail(createEmail.trim())) {
      setCreateError('Format email invalide.');
      return;
    }
    if (!passwordPreview?.trim()) {
      setCreateError('Attendez la proposition de mot de passe ou cliquez sur ↻ pour en générer une.');
      return;
    }
    const payload: UserCreateRequest = {
      firstName: createFirstName.trim(),
      lastName: createLastName.trim(),
      postName: createPostName.trim() || undefined,
      email: createEmail.trim() || undefined,
      role: createRole.trim(),
      generatedPasswordLength: createPasswordLength,
      passwordVariation,
      hospitalServiceIds: createHospitalServiceIds,
    };
    setSubmittingCreate(true);
    setCreateError(null);
    setMessage(null);
    try {
      const { data } = await api.post<UserResponse>('/api/v1/users', payload);
      setShowCreateDrawer(false);
      resetCreateUserForm();
      await loadUsers();
      openDetailForUser(data, data.generatedPassword ?? undefined);
      setMessage('Utilisateur créé.');
    } catch (err) {
      setCreateError(getApiErrorMessage(err, "Impossible de créer l'utilisateur."));
    } finally {
      setSubmittingCreate(false);
    }
  }

  function openDetailForUser(user: UserResponse, password?: string) {
    setDetailError(null);
    setDetailEditing(false);
    setSelectedUser(user);
    setFreshPassword(password ?? user.generatedPassword ?? null);
    setEditFullName(user.fullName);
    setEditEmail(user.email ?? '');
    setEditRole(user.roles[0] ?? roles[0]?.name ?? 'ROLE_RECEPTION');
    setEditHospitalServiceIds([...(user.hospitalServiceIds ?? [])]);
    setEditPassword('');
    setShowCreateDrawer(false);
  }

  function closeDetailDrawer() {
    setSelectedUser(null);
    setFreshPassword(null);
    setEditPassword('');
    setDetailError(null);
    setDetailEditing(false);
    setDetailSubmitReady(false);
    setCredentialsError(null);
  }

  function startDetailEdit() {
    if (!selectedUser || detailEditing) return;
    setDetailEditing(true);
    setEditFullName(selectedUser.fullName);
    setEditEmail(selectedUser.email ?? '');
    setEditRole(selectedUser.roles[0] ?? roles[0]?.name ?? 'ROLE_RECEPTION');
    setEditHospitalServiceIds([...(selectedUser.hospitalServiceIds ?? [])]);
    setEditPassword('');
    setEditPasswordPreview(null);
    setEditPasswordVariation(0);
    setDetailError(null);
  }

  function cancelDetailEdit() {
    if (!selectedUser) return;
    setDetailEditing(false);
    setEditFullName(selectedUser.fullName);
    setEditEmail(selectedUser.email ?? '');
    setEditRole(selectedUser.roles[0] ?? roles[0]?.name ?? 'ROLE_RECEPTION');
    setEditHospitalServiceIds([...(selectedUser.hospitalServiceIds ?? [])]);
    setEditPassword('');
    setEditPasswordPreview(null);
    setEditPasswordVariation(0);
    setDetailError(null);
  }

  function applyEditPasswordSuggestion() {
    if (!editPasswordPreview) return;
    setEditPassword(editPasswordPreview);
  }

  function toggleDetailUser(user: UserResponse) {
    if (selectedUser?.id === user.id) {
      if (detailEditing) {
        cancelDetailEdit();
        return;
      }
      closeDetailDrawer();
      return;
    }
    openDetailForUser(user);
  }

  async function onUpdateUser(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedUser || !detailSubmitReady) return;
    if (!editFullName.trim() || !editRole.trim()) {
      setDetailError('Nom complet et rôle sont requis.');
      return;
    }
    if (editEmail.trim() && !isValidEmail(editEmail.trim())) {
      setDetailError('Format email invalide.');
      return;
    }
    const pwdChanged = editPassword.trim();
    const payload: UserUpdateRequest = {
      fullName: editFullName.trim(),
      email: editEmail.trim() ? editEmail.trim() : null,
      role: editRole.trim(),
      password: pwdChanged || undefined,
      hospitalServiceIds: editHospitalServiceIds,
    };
    setSubmitting(true);
    setDetailError(null);
    try {
      const { data } = await api.put<UserResponse>(`/api/v1/users/${selectedUser.id}`, payload);
      setSelectedUser(data);
      setMessage('Utilisateur mis à jour.');
      setEditPassword('');
      setEditPasswordPreview(null);
      setDetailEditing(false);
      await loadUsers();
      if (pwdChanged) {
        setFreshPassword(pwdChanged);
      } else {
        setFreshPassword(null);
      }
      const creds = await loadUserCredentials(data.id);
      if (creds?.foundInLog && creds.password) {
        setFreshPassword(null);
      }
    } catch (err) {
      setDetailError(getApiErrorMessage(err, "Impossible de mettre à jour l'utilisateur."));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus() {
    if (!selectedUser) return;
    setSubmitting(true);
    setDetailError(null);
    try {
      const { data } = await api.patch<UserResponse>(`/api/v1/users/${selectedUser.id}/status`, {
        active: !selectedUser.active,
      });
      setSelectedUser(data);
      setMessage(data.active ? 'Utilisateur activé.' : 'Utilisateur désactivé.');
      await loadUsers();
    } catch (err) {
      setDetailError(getApiErrorMessage(err, 'Impossible de changer le statut.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function deleteSelectedUser() {
    if (!selectedUser) return;
    if (!window.confirm(`Supprimer l'utilisateur ${selectedUser.username} ?`)) return;
    setSubmitting(true);
    setDetailError(null);
    try {
      await api.delete(`/api/v1/users/${selectedUser.id}`);
      setMessage('Utilisateur supprimé.');
      closeDetailDrawer();
      await loadUsers();
    } catch (err) {
      setDetailError(getApiErrorMessage(err, "Impossible de supprimer l'utilisateur."));
    } finally {
      setSubmitting(false);
    }
  }

  function onToggleSort(column: UserSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  function toggleFilterColumn(column: FilterColumn) {
    setOpenFilterCol((current) => (current === column ? null : column));
    setFilterDraft({ ...appliedFilters });
  }

  function applyFilter(column: FilterColumn) {
    const next = { ...appliedFilters, [column]: filterDraft[column] };
    setAppliedFilters(next);
    if (column === 'username') {
      setSearchInput(filterDraft.username);
    }
    setOpenFilterCol(null);
  }

  function clearFilter(column: FilterColumn) {
    const cleared = column === 'active' ? 'all' : '';
    const next = { ...appliedFilters, [column]: cleared } as ColumnFilters;
    setAppliedFilters(next);
    setFilterDraft((d) => ({ ...d, [column]: cleared }));
    if (column === 'username') {
      setSearchInput('');
    }
    setOpenFilterCol(null);
  }

  function clearAllFilters() {
    setAppliedFilters({ ...emptyFilters });
    setFilterDraft({ ...emptyFilters });
    setSearchInput('');
    setOpenFilterCol(null);
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    const q = searchInput.trim();
    setAppliedFilters((f) => ({ ...f, username: q }));
    setFilterDraft((d) => ({ ...d, username: q }));
  }

  const displayUsers = page?.content ?? [];

  return (
    <>
      <PageHeader
        title="Utilisateurs"
        subtitle="Comptes, rôles et périmètre des services hospitaliers (administration)."
      />

      <div className="card filter-toolbar">
        <div className="filter-toolbar__inner">
          <form onSubmit={onSearchSubmit} className="filter-toolbar__form">
            <div className="field field--grow">
              <label htmlFor="u-search">Recherche</label>
              <input
                id="u-search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Identifiant, nom, email…"
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
              closeDetailDrawer();
              setShowCreateDrawer(true);
              setCreateError(null);
            }}
          >
            + Nouvel utilisateur
          </button>
        </div>
      </div>

      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error && <div className="error-banner">{error}</div>}
      {filterHint ? (
        <DataTableFilterHint>
          Filtres : {filterHint}
          {' — '}
          <button
            type="button"
            className="btn btn-ghost"
            style={{ padding: 0, verticalAlign: 'baseline' }}
            onClick={clearAllFilters}
          >
            Tout effacer
          </button>
        </DataTableFilterHint>
      ) : null}

      {loading && <LoadingBlock label="Chargement des utilisateurs…" />}

      {!loading && page && (
        <div className="card table-wrap">
          {page.content.length === 0 ? (
            <EmptyState
              title="Aucun utilisateur"
              description={
                hasActiveFilters
                  ? 'Aucun résultat pour ces filtres — élargissez les critères ou réinitialisez.'
                  : 'Créez un nouveau compte ou importez des utilisateurs via l’administration.'
              }
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
                          label="Identifiant"
                          sortable
                          sortActive={sortBy === 'username'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('username')}
                          filterable
                          filterOpen={openFilterCol === 'username'}
                          filterActive={appliedFilters.username.trim() !== ''}
                          onToggleFilter={() => toggleFilterColumn('username')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader label="Email" />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Nom complet"
                          sortable
                          sortActive={sortBy === 'fullName'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('fullName')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Rôle"
                          sortable
                          sortActive={sortBy === 'role'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('role')}
                          filterable
                          filterOpen={openFilterCol === 'role'}
                          filterActive={appliedFilters.role !== ''}
                          onToggleFilter={() => toggleFilterColumn('role')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Services"
                          filterable
                          filterOpen={openFilterCol === 'service'}
                          filterActive={appliedFilters.service !== ''}
                          onToggleFilter={() => toggleFilterColumn('service')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Statut"
                          sortable
                          sortActive={sortBy === 'active'}
                          sortDir={sortDir}
                          onSort={() => onToggleSort('active')}
                          filterable
                          filterOpen={openFilterCol === 'active'}
                          filterActive={appliedFilters.active !== 'all'}
                          onToggleFilter={() => toggleFilterColumn('active')}
                        />
                      </th>
                      <th>Détail</th>
                    </tr>
                    {showFilterRow ? (
                      <tr className="table-filter-row">
                        <th />
                        <th>
                          <DataTableFilterCell
                            open={openFilterCol === 'username'}
                            value={filterDraft.username}
                            onChange={(v) => setFilterDraft((d) => ({ ...d, username: v }))}
                            onApply={() => applyFilter('username')}
                            onClear={() => clearFilter('username')}
                            placeholder="Identifiant, nom, email…"
                          />
                        </th>
                        <th />
                        <th />
                        <th>
                          <DataTableFilterSelectCell
                            open={openFilterCol === 'role'}
                            value={filterDraft.role}
                            onChange={(v) => setFilterDraft((d) => ({ ...d, role: v }))}
                            onApply={() => applyFilter('role')}
                            onClear={() => clearFilter('role')}
                            options={roleFilterOptions}
                            ariaLabel="Filtrer par rôle"
                            allLabel="Tous les rôles"
                          />
                        </th>
                        <th>
                          <DataTableFilterSelectCell
                            open={openFilterCol === 'service'}
                            value={filterDraft.service}
                            onChange={(v) => setFilterDraft((d) => ({ ...d, service: v }))}
                            onApply={() => applyFilter('service')}
                            onClear={() => clearFilter('service')}
                            options={serviceFilterOptions}
                            ariaLabel="Filtrer par service"
                            allLabel="Tous les services"
                          />
                        </th>
                        <th>
                          <DataTableFilterSelectCell
                            open={openFilterCol === 'active'}
                            value={filterDraft.active === 'all' ? '' : filterDraft.active}
                            onChange={(v) =>
                              setFilterDraft((d) => ({
                                ...d,
                                active: (v || 'all') as UserActiveFilter,
                              }))
                            }
                            onApply={() => applyFilter('active')}
                            onClear={() => clearFilter('active')}
                            options={activeFilterOptions}
                            ariaLabel="Filtrer par statut"
                            allLabel="Tous les statuts"
                          />
                        </th>
                        <th />
                      </tr>
                    ) : null}
                  </thead>
                  <tbody>
                    {displayUsers.map((u) => (
                      <tr
                        key={u.id}
                        className={
                          selectedUser?.id === u.id
                            ? 'data-table-row--selected'
                            : 'data-table-row--clickable'
                        }
                        onClick={() => toggleDetailUser(u)}
                      >
                        <td className="data-table-col--id">{u.id}</td>
                        <td>
                          <code className="user-username">{u.username}</code>
                        </td>
                        <td>{u.email || '—'}</td>
                        <td>{u.fullName}</td>
                        <td>{u.roles.map((r) => roleLabelFor(roles, r)).join(', ')}</td>
                        <td
                          className="data-table-col--truncate"
                          title={serviceNamesFor(u.hospitalServiceIds ?? [], hospitalServicesCatalog)}
                        >
                          {serviceNamesFor(u.hospitalServiceIds ?? [], hospitalServicesCatalog)}
                        </td>
                        <td>
                          <span
                            className={
                              u.active
                                ? 'patient-status-badge patient-status-badge--active'
                                : 'patient-status-badge patient-status-badge--discharged'
                            }
                          >
                            {u.active ? 'Actif' : 'Inactif'}
                          </span>
                        </td>
                        <td>
                          <span className="user-row-voir">Voir</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </ScrollTableRegion>
              <TableResultFooter
                totalElements={page.totalElements}
                displayedCount={page.content.length}
                itemLabelPlural="utilisateur(s)"
              />
            </>
          )}
        </div>
      )}

      <Drawer
        open={selectedUser != null}
        onClose={closeDetailDrawer}
        title={selectedUser ? selectedUser.fullName : 'Utilisateur'}
        width="lg"
        footer={
          selectedUser ? (
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
              {detailEditing ? (
                <>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={cancelDetailEdit}
                    disabled={submitting}
                  >
                    Annuler
                  </button>
                  <button
                    type="submit"
                    form="user-detail-form"
                    className="btn btn-primary"
                    disabled={submitting || !detailSubmitReady}
                  >
                    {submitting ? 'Enregistrement…' : 'Enregistrer'}
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={startDetailEdit}
                  >
                    Modifier
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => void toggleStatus()}
                    disabled={submitting || isSelf}
                  >
                    {selectedUser.active ? 'Désactiver' : 'Activer'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => void deleteSelectedUser()}
                    disabled={submitting || isSelf}
                  >
                    Supprimer
                  </button>
                </>
              )}
              <button type="button" className="btn btn-ghost" onClick={closeDetailDrawer} disabled={submitting}>
                Fermer
              </button>
            </div>
          ) : null
        }
      >
        {selectedUser && (
          <form id="user-detail-form" onSubmit={onUpdateUser} className="card card--flat">
            {detailError && <div className="error-banner">{detailError}</div>}
            <dl className="user-detail-summary">
              <div>
                <dt>ID</dt>
                <dd>{selectedUser.id}</dd>
              </div>
              <div>
                <dt>Identifiant de connexion</dt>
                <dd>
                  <code className="user-username">{selectedUser.username}</code>
                  <span className="field-hint" style={{ display: 'block', marginTop: '0.2rem' }}>
                    Saisi tel quel à la connexion (sans espace).
                  </span>
                </dd>
              </div>
              <div>
                <dt>Statut</dt>
                <dd>
                  <span
                    className={
                      selectedUser.active
                        ? 'patient-status-badge patient-status-badge--active'
                        : 'patient-status-badge patient-status-badge--discharged'
                    }
                  >
                    {selectedUser.active ? 'Actif' : 'Inactif'}
                  </span>
                </dd>
              </div>
              {!detailEditing ? (
                <div>
                  <dt>Rôle</dt>
                  <dd>{selectedUser.roles.map((r) => roleLabelFor(roles, r)).join(', ')}</dd>
                </div>
              ) : null}
            </dl>

            {!detailEditing ? (
              <>
                <div className="field" style={{ marginBottom: '0.75rem' }}>
                  <label>Nom complet</label>
                  <p style={{ margin: 0 }}>{selectedUser.fullName}</p>
                </div>
                <div className="field" style={{ marginBottom: '0.75rem' }}>
                  <label>Email</label>
                  <p style={{ margin: 0 }}>{selectedUser.email || '—'}</p>
                </div>
                <div className="field" style={{ marginBottom: '0.75rem' }}>
                  <label>Mot de passe</label>
                  {credentialsLoading && !displayedPassword ? (
                    <p className="muted-text" style={{ margin: 0 }}>
                      Chargement…
                    </p>
                  ) : displayedPassword ? (
                    <div className="user-detail-drawer__password">
                      <code className="credentials-password-block">{displayedPassword}</code>
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => void navigator.clipboard.writeText(displayedPassword)}
                      >
                        Copier
                      </button>
                      {selectedCredentials?.loggedAt ? (
                        <span className="field-hint">Enregistré le {selectedCredentials.loggedAt}</span>
                      ) : freshPassword ? (
                        <span className="field-hint">Mot de passe affiché à la création du compte.</span>
                      ) : null}
                    </div>
                  ) : (
                    <p className="muted-text" style={{ margin: 0 }}>
                      {credentialsError ??
                        'Non disponible (compte créé avant le journal, mot de passe modifié, ou journal inaccessible).'}
                    </p>
                  )}
                </div>
              </>
            ) : (
              <>
                <section className="drawer-form-section">
                  <h3 className="drawer-form-section__title">Identité et accès</h3>
                  <div className="form-grid">
                    <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                      <label htmlFor="u-detail-fullname">Nom complet *</label>
                      <input
                        id="u-detail-fullname"
                        value={editFullName}
                        onChange={(e) => setEditFullName(e.target.value)}
                        required
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="u-detail-email">Email</label>
                      <input
                        id="u-detail-email"
                        type="email"
                        value={editEmail}
                        onChange={(e) => setEditEmail(e.target.value)}
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="u-detail-role">Rôle *</label>
                      <select
                        id="u-detail-role"
                        value={editRole}
                        onChange={(e) => setEditRole(e.target.value)}
                        required
                      >
                        {roles.map((role) => (
                          <option key={role.id} value={role.name}>
                            {role.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                      <label htmlFor="u-detail-services">Services hospitaliers</label>
                      <select
                        id="u-detail-services"
                        multiple
                        value={editHospitalServiceIds.map(String)}
                        onChange={(e) => {
                          const opts = Array.from(e.target.selectedOptions);
                          setEditHospitalServiceIds(opts.map((o) => Number(o.value)));
                        }}
                        className="select-multiline"
                      >
                        {hospitalServicesCatalog.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name}
                          </option>
                        ))}
                      </select>
                      <span className="field-hint">
                        Affectation actuelle :{' '}
                        {serviceNamesFor(editHospitalServiceIds, hospitalServicesCatalog)}. Ctrl ou ⌘ pour
                        sélection multiple.
                      </span>
                    </div>
                  </div>
                </section>
                <section className="drawer-form-section">
                  <h3 className="drawer-form-section__title">Mot de passe</h3>
                  <div className="form-grid">
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="u-detail-pwd-len">Longueur (suggestion)</label>
                      <select
                        id="u-detail-pwd-len"
                        value={editPasswordLength}
                        onChange={(e) => setEditPasswordLength(Number(e.target.value) as 12 | 16)}
                      >
                        <option value={12}>12 caractères</option>
                        <option value={16}>16 caractères</option>
                      </select>
                    </div>
                    <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                      <label>Suggestion</label>
                      {editPwdSuggestLoading ? (
                        <p className="muted-text" style={{ margin: 0 }}>
                          Génération…
                        </p>
                      ) : editPasswordPreview ? (
                        <div className="user-detail-drawer__password">
                          <code className="credentials-password-block">{editPasswordPreview}</code>
                          <div className="user-create-form__password-actions">
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              onClick={() => void navigator.clipboard.writeText(editPasswordPreview)}
                            >
                              Copier
                            </button>
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              onClick={applyEditPasswordSuggestion}
                            >
                              Utiliser
                            </button>
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              title="Autre combinaison"
                              aria-label="Régénérer le mot de passe"
                              disabled={!editFullName.trim()}
                              onClick={() => void fetchEditPasswordSuggestion(editPasswordVariation + 1)}
                            >
                              Autre combinaison
                            </button>
                          </div>
                        </div>
                      ) : (
                        <p className="muted-text" style={{ margin: 0 }}>
                          Saisissez un nom complet pour obtenir une suggestion.
                        </p>
                      )}
                    </div>
                    <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                      <label htmlFor="u-detail-password">Nouveau mot de passe</label>
                      <input
                        id="u-detail-password"
                        type="text"
                        value={editPassword}
                        onChange={(e) => setEditPassword(e.target.value)}
                        placeholder="Laisser vide pour ne pas changer"
                        autoComplete="new-password"
                      />
                      <span className="field-hint">
                        Si renseigné, le mot de passe sera enregistré dans le journal après validation.
                      </span>
                    </div>
                  </div>
                </section>
              </>
            )}
          </form>
        )}
      </Drawer>

      <Drawer
        open={showCreateDrawer}
        onClose={closeCreateDrawer}
        title="Nouvel utilisateur"
        width="lg"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button
              type="submit"
              form="user-create-form"
              className="btn btn-primary"
              disabled={submittingCreate || pwdSuggestLoading}
            >
              {submittingCreate ? 'Création…' : 'Créer le compte'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={closeCreateDrawer} disabled={submittingCreate}>
              Annuler
            </button>
          </div>
        }
      >
        {createError && <div className="error-banner">{createError}</div>}
        <form id="user-create-form" onSubmit={onCreateUser} className="card card--flat user-create-form">
          <section className="drawer-form-section">
            <h3 className="drawer-form-section__title">Identité</h3>
            <div className="form-grid">
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="u-firstname">Prénom *</label>
                <input
                  id="u-firstname"
                  value={createFirstName}
                  onChange={(e) => setCreateFirstName(e.target.value)}
                  required
                  autoFocus
                />
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="u-lastname">Nom *</label>
                <input
                  id="u-lastname"
                  value={createLastName}
                  onChange={(e) => setCreateLastName(e.target.value)}
                  required
                />
              </div>
              <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                <label htmlFor="u-postname">Post-nom</label>
                <input
                  id="u-postname"
                  value={createPostName}
                  onChange={(e) => setCreatePostName(e.target.value)}
                />
              </div>
            </div>
            {(suggestedFullName || suggestedUsername !== '—') && (
              <div className="user-create-form__preview muted-text">
                {suggestedFullName ? (
                  <p style={{ margin: '0.75rem 0 0.25rem' }}>
                    Nom affiché : <strong>{suggestedFullName}</strong>
                  </p>
                ) : null}
                <p style={{ margin: suggestedFullName ? '0' : '0.75rem 0 0' }}>
                  Identifiant prévu :{' '}
                  <code className="user-username">{suggestedUsername}</code>
                  <span className="field-hint" style={{ display: 'block', marginTop: '0.2rem' }}>
                    Prénom + nom, sans point (ex. receptionkas). Un chiffre peut être ajouté si déjà pris.
                  </span>
                </p>
              </div>
            )}
          </section>

          <section className="drawer-form-section">
            <h3 className="drawer-form-section__title">Compte et accès</h3>
            <div className="form-grid">
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="u-email">Email</label>
                <input
                  id="u-email"
                  type="email"
                  value={createEmail}
                  onChange={(e) => setCreateEmail(e.target.value)}
                />
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="u-role">Rôle *</label>
                <select
                  id="u-role"
                  value={createRole}
                  onChange={(e) => setCreateRole(e.target.value)}
                  required
                >
                  {roles.length === 0 && <option value="">Chargement…</option>}
                  {roles.map((role) => (
                    <option key={role.id} value={role.name}>
                      {role.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                <label htmlFor="u-hospital-services-create">Services hospitaliers</label>
                <select
                  id="u-hospital-services-create"
                  multiple
                  value={createHospitalServiceIds.map(String)}
                  onChange={(e) => {
                    const opts = Array.from(e.target.selectedOptions);
                    setCreateHospitalServiceIds(opts.map((o) => Number(o.value)));
                  }}
                  className="select-multiline"
                >
                  {hospitalServicesCatalog.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
                <span className="field-hint">
                  Médecins et infirmiers : limite les admissions visibles. Ctrl ou ⌘ pour sélection multiple.
                </span>
              </div>
            </div>
          </section>

          <section className="drawer-form-section">
            <h3 className="drawer-form-section__title">Mot de passe</h3>
            <div className="form-grid">
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="u-pwd-len">Longueur</label>
                <select
                  id="u-pwd-len"
                  value={createPasswordLength}
                  onChange={(e) => setCreatePasswordLength(Number(e.target.value) as 12 | 16)}
                >
                  <option value={12}>12 caractères</option>
                  <option value={16}>16 caractères</option>
                </select>
              </div>
              <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                <label>Mot de passe généré</label>
                {pwdSuggestLoading ? (
                  <p className="muted-text" style={{ margin: 0 }}>
                    Génération…
                  </p>
                ) : passwordPreview ? (
                  <div className="user-detail-drawer__password">
                    <code className="credentials-password-block">{passwordPreview}</code>
                    <div className="user-create-form__password-actions">
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        onClick={() => void navigator.clipboard.writeText(passwordPreview)}
                      >
                        Copier
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost btn-sm"
                        title="Autre combinaison"
                        aria-label="Régénérer le mot de passe"
                        disabled={!createFirstName.trim() || !createLastName.trim()}
                        onClick={() => void fetchPasswordSuggestion(passwordVariation + 1)}
                      >
                        Autre combinaison
                      </button>
                    </div>
                    <span className="field-hint">
                      Communiqué une seule fois à l&apos;utilisateur ; recopié dans le journal à la création.
                    </span>
                  </div>
                ) : (
                  <p className="muted-text" style={{ margin: 0 }}>
                    Saisissez le prénom et le nom pour générer un mot de passe.
                  </p>
                )}
              </div>
            </div>
          </section>
        </form>
      </Drawer>

    </>
  );
}
