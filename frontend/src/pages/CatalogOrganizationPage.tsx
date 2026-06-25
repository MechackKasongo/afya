import { Fragment, useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import type {
  DepartmentRequest,
  DepartmentResponse,
  HospitalServiceRequest,
  BedOccupationResponse,
  BedResponse,
  HospitalServiceResponse,
  PageHospitalServiceResponse,
} from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Toast } from '../components/ui/Toast';
import { departmentCodeFromName, departmentCodeWithSuffix } from '../utils/departmentCode';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import { formatAuditDateTime } from '../utils/reporting';
function servicesForDepartment(services: HospitalServiceResponse[], departmentId: number) {
  return services.filter((s) => s.departmentId === departmentId);
}

export function CatalogOrganizationPage() {
  const { user } = useAuth();
  const isAdmin = hasRole(user, 'ROLE_ADMIN');

  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [deptName, setDeptName] = useState('');
  const [editingDept, setEditingDept] = useState(false);
  const [editDeptName, setEditDeptName] = useState('');

  const [showAddService, setShowAddService] = useState(false);
  const [newServiceName, setNewServiceName] = useState('');
  const [newServiceBeds, setNewServiceBeds] = useState('');
  const [newServiceBedsPerRoom, setNewServiceBedsPerRoom] = useState('1');
  const [newServiceRoomLetter, setNewServiceRoomLetter] = useState('A');

  const [editServiceId, setEditServiceId] = useState<number | null>(null);
  const [editServiceName, setEditServiceName] = useState('');
  const [editServiceBeds, setEditServiceBeds] = useState('');
  const [editServiceBedsPerRoom, setEditServiceBedsPerRoom] = useState('1');
  const [editServiceRoomLetter, setEditServiceRoomLetter] = useState('A');
  const [expandedServiceId, setExpandedServiceId] = useState<number | null>(null);
  const [bedsByService, setBedsByService] = useState<Record<number, BedResponse[]>>({});
  const [occupationsByService, setOccupationsByService] = useState<
    Record<number, BedOccupationResponse[]>
  >({});

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [deptRes, svcRes] = await Promise.all([
        api.get<DepartmentResponse[]>('/api/v1/departments'),
        api.get<PageHospitalServiceResponse>(
          `/api/v1/hospital-services?page=0&size=${LIST_FETCH_PAGE_SIZE}`
        ),
      ]);
      setDepartments(deptRes.data);
      setServices(svcRes.data.content);
      if (deptRes.data.length === 0 && svcRes.data.content.length > 0) {
        setError(
          'Les services sont visibles mais pas les départements. Redémarrez afya-bff (./mvnw -pl afya-bff spring-boot:run) puis rechargez la page.'
        );
      }
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Impossible de charger l’organisation. Redémarrez afya-bff (8080) et hospital-service (8082).'
        )
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const sortedDepartments = useMemo(
    () => [...departments].sort((a, b) => a.name.localeCompare(b.name, 'fr')),
    [departments]
  );

  const selectedDept = useMemo(
    () => sortedDepartments.find((d) => d.id === selectedDeptId) ?? null,
    [sortedDepartments, selectedDeptId]
  );

  const selectedServices = useMemo(
    () => (selectedDept ? servicesForDepartment(services, selectedDept.id) : []),
    [selectedDept, services]
  );

  useEffect(() => {
    if (loading || sortedDepartments.length === 0) {
      return;
    }
    if (selectedDeptId == null || !sortedDepartments.some((d) => d.id === selectedDeptId)) {
      setSelectedDeptId(sortedDepartments[0].id);
    }
  }, [loading, sortedDepartments, selectedDeptId]);

  function selectDepartment(deptId: number) {
    setSelectedDeptId(deptId);
    setEditingDept(false);
    setShowAddService(false);
    setEditServiceId(null);
    setExpandedServiceId(null);
  }

  async function loadBedsForService(serviceId: number) {
    try {
      const { data } = await api.get<BedResponse[]>(`/api/v1/hospital-services/${serviceId}/beds`);
      setBedsByService((prev) => ({ ...prev, [serviceId]: data }));
    } catch {
      setBedsByService((prev) => ({ ...prev, [serviceId]: [] }));
    }
  }

  async function loadOccupationsForService(serviceId: number) {
    try {
      const { data } = await api.get<BedOccupationResponse[]>(
        `/api/v1/hospital-services/${serviceId}/bed-occupations`,
        { params: { size: 15 } },
      );
      setOccupationsByService((prev) => ({ ...prev, [serviceId]: data }));
    } catch {
      setOccupationsByService((prev) => ({ ...prev, [serviceId]: [] }));
    }
  }

  async function toggleServiceBeds(svc: HospitalServiceResponse) {
    if (expandedServiceId === svc.id) {
      setExpandedServiceId(null);
      return;
    }
    setExpandedServiceId(svc.id);
    if (!bedsByService[svc.id]) {
      await loadBedsForService(svc.id);
    }
    if (!occupationsByService[svc.id]) {
      await loadOccupationsForService(svc.id);
    }
  }

  async function provisionBedsForService(svc: HospitalServiceResponse) {
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const { data: created } = await api.post<number>(
        `/api/v1/hospital-services/${svc.id}/beds/provision`
      );
      setMessage(`${created} lit(s) créé(s) pour « ${svc.name} ».`);
      await loadAll();
      await loadBedsForService(svc.id);
      await loadOccupationsForService(svc.id);
      setExpandedServiceId(svc.id);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de générer les lits.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function realignBedsForService(svc: HospitalServiceResponse) {
    if (
      !window.confirm(
        `Recréer les lits libres de « ${svc.name} » au format actuel (ex. ${svc.roomLetterPrefix ?? 'A'}1-01) ?\n\nLes lits occupés par un patient sont conservés.`
      )
    ) {
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const { data: created } = await api.post<number>(
        `/api/v1/hospital-services/${svc.id}/beds/realign`
      );
      setMessage(`${created} lit(s) recréé(s) pour « ${svc.name} » (format chambre + lit à jour).`);
      await loadAll();
      await loadBedsForService(svc.id);
      await loadOccupationsForService(svc.id);
      setExpandedServiceId(svc.id);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de recréer les lits.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function postDepartmentWithGeneratedCode(name: string, active: boolean): Promise<DepartmentResponse> {
    const base = departmentCodeFromName(name);
    for (let attempt = 1; attempt <= 10; attempt++) {
      const code = departmentCodeWithSuffix(base, attempt);
      try {
        const { data } = await api.post<DepartmentResponse>('/api/v1/departments', {
          name,
          code,
          active,
        } satisfies DepartmentRequest);
        return data;
      } catch (err) {
        const msg = getApiErrorMessage(err, '');
        if (attempt < 10 && /déjà utilisé|already|409|conflict/i.test(msg)) {
          continue;
        }
        throw err;
      }
    }
    throw new Error('Impossible de générer un code département unique');
  }

  async function onCreateDepartment(e: React.FormEvent) {
    e.preventDefault();
    if (!deptName.trim()) {
      setError('Nom du département requis.');
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const name = deptName.trim();
      const created = await postDepartmentWithGeneratedCode(name, true);
      setDeptName('');
      setMessage(`Département « ${name} » créé.`);
      await loadAll();
      setSelectedDeptId(created.id);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Impossible de créer le département. Redémarrez hospital-service et afya-bff si le problème persiste.'
        )
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function onUpdateDepartment(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedDept || !editDeptName.trim()) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.put(`/api/v1/departments/${selectedDept.id}`, {
        code: selectedDept.code,
        name: editDeptName.trim(),
        active: selectedDept.active,
      } satisfies DepartmentRequest);
      setEditingDept(false);
      setMessage('Département modifié.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de modifier le département.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleDepartment(item: DepartmentResponse) {
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.put(`/api/v1/departments/${item.id}`, {
        code: item.code,
        name: item.name,
        active: !item.active,
      });
      setMessage(item.active ? 'Département désactivé.' : 'Département activé.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de changer le statut du département.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function removeDepartment(item: DepartmentResponse) {
    const linked = servicesForDepartment(services, item.id);
    if (linked.length > 0) {
      setError(
        `Impossible de supprimer : ${linked.length} service(s) encore rattaché(s). Supprimez-les d’abord dans ce département.`
      );
      return;
    }
    if (!window.confirm(`Supprimer le département « ${item.name} » (${item.code}) ?`)) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.delete(`/api/v1/departments/${item.id}`);
      setMessage('Département supprimé.');
      if (selectedDeptId === item.id) {
        setSelectedDeptId(null);
      }
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer le département.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function onCreateService(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedDept) return;
    const beds = Number.parseInt(newServiceBeds, 10);
    const bedsPerRoom = Number.parseInt(newServiceBedsPerRoom, 10);
    if (
      !newServiceName.trim() ||
      !Number.isFinite(beds) ||
      beds <= 0 ||
      !Number.isFinite(bedsPerRoom) ||
      bedsPerRoom <= 0
    ) {
      setError('Nom, nombre total de lits (> 0) et lits par chambre (> 0) requis.');
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.post('/api/v1/hospital-services', {
        departmentId: selectedDept.id,
        name: newServiceName.trim(),
        bedCapacity: beds,
        bedsPerRoom,
        roomLetterPrefix: newServiceRoomLetter.trim().toUpperCase().slice(0, 1) || 'A',
      } satisfies HospitalServiceRequest);
      setShowAddService(false);
      setNewServiceName('');
      setNewServiceBeds('');
      setNewServiceBedsPerRoom('1');
      setNewServiceRoomLetter('A');
      setMessage('Service créé.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer le service.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function onUpdateService(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedDept || editServiceId == null) return;
    const beds = Number.parseInt(editServiceBeds, 10);
    const bedsPerRoom = Number.parseInt(editServiceBedsPerRoom, 10);
    if (
      !editServiceName.trim() ||
      !Number.isFinite(beds) ||
      beds <= 0 ||
      !Number.isFinite(bedsPerRoom) ||
      bedsPerRoom <= 0
    ) {
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.put(`/api/v1/hospital-services/${editServiceId}`, {
        departmentId: selectedDept.id,
        name: editServiceName.trim(),
        bedCapacity: beds,
        bedsPerRoom,
        roomLetterPrefix: editServiceRoomLetter.trim().toUpperCase().slice(0, 1) || 'A',
      } satisfies HospitalServiceRequest);
      setEditServiceId(null);
      setMessage('Service modifié.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de modifier le service.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleService(item: HospitalServiceResponse) {
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.patch(`/api/v1/hospital-services/${item.id}/status`, { active: !item.active });
      setMessage(item.active ? 'Service désactivé.' : 'Service activé.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de changer le statut du service.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function removeService(item: HospitalServiceResponse) {
    if (
      !window.confirm(
        `Supprimer le service « ${item.name} » ?\n\nÀ n’utiliser que s’il n’a jamais servi pour des admissions. Sinon, préférez Désactiver.`
      )
    ) {
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.delete(`/api/v1/hospital-services/${item.id}`);
      setMessage('Service supprimé.');
      await loadAll();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer le service.'));
    } finally {
      setSubmitting(false);
    }
  }

  function startEditDepartment() {
    if (!selectedDept) return;
    setEditDeptName(selectedDept.name);
    setEditingDept(true);
    setShowAddService(false);
    setEditServiceId(null);
  }

  function startEditService(item: HospitalServiceResponse) {
    setEditServiceId(item.id);
    setEditServiceName(item.name);
    setEditServiceBeds(String(item.bedCapacity));
    setEditServiceBedsPerRoom(String(item.bedsPerRoom ?? 1));
    setEditServiceRoomLetter((item.roomLetterPrefix ?? 'A').slice(0, 1));
    setShowAddService(false);
    setEditingDept(false);
  }

  const canDeleteSelectedDept = isAdmin && selectedDept && selectedServices.length === 0;

  return (
    <>
      <PageHeader title="Organisation hospitalière" />

      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error && <div className="error-banner">{error}</div>}

      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && (
        <div className="catalog-org-layout">
          <aside className="card catalog-org-list">
            <div className="catalog-org-list__head">
              <h3>Départements</h3>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--muted)' }}>
                {sortedDepartments.length} au total
              </p>
            </div>

            {isAdmin && (
              <div className="catalog-org-list__create">
                <form onSubmit={onCreateDepartment}>
                  <div className="field" style={{ marginBottom: '0.5rem' }}>
                    <label htmlFor="org-dept-name">Nouveau département</label>
                    <input
                      id="org-dept-name"
                      value={deptName}
                      onChange={(e) => setDeptName(e.target.value)}
                      placeholder="Ex. Pédiatrie"
                      maxLength={120}
                    />
                  </div>
                  <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={submitting}>
                    {submitting ? '…' : 'Ajouter'}
                  </button>
                </form>
              </div>
            )}

            {sortedDepartments.length === 0 ? (
              <p style={{ padding: '1rem', margin: 0, color: 'var(--muted)', fontSize: '0.9rem' }}>
                Aucun département.
              </p>
            ) : (
              <ul className="catalog-org-list__items" role="listbox" aria-label="Départements">
                {sortedDepartments.map((dept) => {
                  const count = servicesForDepartment(services, dept.id).length;
                  const isSelected = dept.id === selectedDeptId;
                  return (
                    <li key={dept.id} role="presentation">
                      <button
                        type="button"
                        role="option"
                        aria-selected={isSelected}
                        className={
                          isSelected
                            ? 'catalog-org-list__item catalog-org-list__item--active'
                            : 'catalog-org-list__item'
                        }
                        onClick={() => selectDepartment(dept.id)}
                      >
                        <span className="catalog-org-list__item-title">{dept.name}</span>
                        <span className="catalog-org-list__item-meta">
                          <span>{dept.code}</span>
                          <span>{count} service{count !== 1 ? 's' : ''}</span>
                          <span>{dept.active ? 'Actif' : 'Inactif'}</span>
                        </span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </aside>

          <section className="card catalog-org-detail">
            {!selectedDept ? (
              <div className="catalog-org-detail__empty">
                Sélectionnez un département dans la liste, ou créez-en un nouveau.
              </div>
            ) : editingDept && isAdmin ? (
              <>
                <div className="catalog-org-detail__header">
                  <h3 style={{ margin: 0 }}>Modifier le département</h3>
                </div>
                <div className="catalog-org-detail__body">
                  <form onSubmit={onUpdateDepartment} className="catalog-org-inline-form">
                    <div className="field" style={{ flex: '1 1 240px', marginBottom: 0 }}>
                      <label htmlFor="org-edit-dept-name">Nom *</label>
                      <input
                        id="org-edit-dept-name"
                        value={editDeptName}
                        onChange={(e) => setEditDeptName(e.target.value)}
                        required
                      />
                    </div>
                    <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--muted)', alignSelf: 'center' }}>
                      Code : {selectedDept.code}
                    </p>
                    <button type="submit" className="btn btn-primary" disabled={submitting}>
                      Enregistrer
                    </button>
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => setEditingDept(false)}
                      disabled={submitting}
                    >
                      Annuler
                    </button>
                  </form>
                </div>
              </>
            ) : (
              <>
                <div className="catalog-org-detail__header">
                  <div>
                    <h2 style={{ margin: '0 0 0.35rem', fontSize: '1.25rem' }}>{selectedDept.name}</h2>
                    <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--muted)' }}>
                      Code {selectedDept.code} · {selectedDept.active ? 'Actif' : 'Inactif'} ·{' '}
                      {selectedServices.length} service{selectedServices.length !== 1 ? 's' : ''}
                    </p>
                  </div>
                  {isAdmin && (
                    <div className="catalog-org-detail__actions">
                      <button type="button" className="btn btn-ghost" onClick={startEditDepartment}>
                        Modifier
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        onClick={() => void toggleDepartment(selectedDept)}
                        disabled={submitting}
                      >
                        {selectedDept.active ? 'Désactiver' : 'Activer'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger"
                        onClick={() => void removeDepartment(selectedDept)}
                        disabled={submitting || !canDeleteSelectedDept}
                        title={
                          canDeleteSelectedDept
                            ? 'Supprimer ce département'
                            : 'Supprimez d’abord tous les services'
                        }
                      >
                        Supprimer
                      </button>
                      {selectedDept.active && (
                        <button
                          type="button"
                          className="btn btn-primary"
                          onClick={() => {
                            setShowAddService(true);
                            setEditServiceId(null);
                            setNewServiceName('');
                            setNewServiceBeds('');
                            setNewServiceBedsPerRoom('1');
                            setNewServiceRoomLetter('A');
                          }}
                        >
                          + Service
                        </button>
                      )}
                    </div>
                  )}
                </div>

                <div className="catalog-org-detail__body">
                  {isAdmin && showAddService && selectedDept.active && (
                    <form onSubmit={onCreateService} className="catalog-org-inline-form">
                      <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
                        <label htmlFor="org-new-svc-name">Nouveau service *</label>
                        <input
                          id="org-new-svc-name"
                          value={newServiceName}
                          onChange={(e) => setNewServiceName(e.target.value)}
                          placeholder="Ex. Unité coronarienne"
                          required
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 120px', marginBottom: 0 }}>
                        <label htmlFor="org-new-svc-beds">Nombre de lits *</label>
                        <input
                          id="org-new-svc-beds"
                          type="number"
                          min={1}
                          value={newServiceBeds}
                          onChange={(e) => setNewServiceBeds(e.target.value)}
                          required
                          title="Chaque lit compte (ex. 10 lits au total)"
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 72px', marginBottom: 0 }}>
                        <label htmlFor="org-new-svc-rl">Lettre ch.</label>
                        <input
                          id="org-new-svc-rl"
                          value={newServiceRoomLetter}
                          onChange={(e) =>
                            setNewServiceRoomLetter(e.target.value.toUpperCase().slice(0, 1))
                          }
                          maxLength={1}
                          pattern="[A-Za-z]"
                          required
                          title="Lettre avant le numéro de chambre (ex. A → A1, A2)"
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 120px', marginBottom: 0 }}>
                        <label htmlFor="org-new-svc-bpr">Lits / chambre *</label>
                        <input
                          id="org-new-svc-bpr"
                          type="number"
                          min={1}
                          max={99}
                          value={newServiceBedsPerRoom}
                          onChange={(e) => setNewServiceBedsPerRoom(e.target.value)}
                          required
                          title="Ex. 2 lits/chambre → A1-01, A1-02 (même chambre A1)"
                        />
                      </div>
                      <button type="submit" className="btn btn-primary" disabled={submitting}>
                        Créer
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        onClick={() => setShowAddService(false)}
                        disabled={submitting}
                      >
                        Annuler
                      </button>
                    </form>
                  )}

                  {isAdmin && editServiceId != null && (
                    <form onSubmit={onUpdateService} className="catalog-org-inline-form">
                      <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
                        <label htmlFor="org-edit-svc-name">Modifier le service *</label>
                        <input
                          id="org-edit-svc-name"
                          value={editServiceName}
                          onChange={(e) => setEditServiceName(e.target.value)}
                          required
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 120px', marginBottom: 0 }}>
                        <label htmlFor="org-edit-svc-beds">Nombre de lits *</label>
                        <input
                          id="org-edit-svc-beds"
                          type="number"
                          min={1}
                          value={editServiceBeds}
                          onChange={(e) => setEditServiceBeds(e.target.value)}
                          required
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 72px', marginBottom: 0 }}>
                        <label htmlFor="org-edit-svc-rl">Lettre ch.</label>
                        <input
                          id="org-edit-svc-rl"
                          value={editServiceRoomLetter}
                          onChange={(e) =>
                            setEditServiceRoomLetter(e.target.value.toUpperCase().slice(0, 1))
                          }
                          maxLength={1}
                          pattern="[A-Za-z]"
                          required
                        />
                      </div>
                      <div className="field" style={{ flex: '0 0 120px', marginBottom: 0 }}>
                        <label htmlFor="org-edit-svc-bpr">Lits / chambre *</label>
                        <input
                          id="org-edit-svc-bpr"
                          type="number"
                          min={1}
                          max={99}
                          value={editServiceBedsPerRoom}
                          onChange={(e) => setEditServiceBedsPerRoom(e.target.value)}
                          required
                        />
                      </div>
                      <button type="submit" className="btn btn-primary" disabled={submitting}>
                        Enregistrer
                      </button>
                      <button
                        type="button"
                        className="btn btn-ghost"
                        onClick={() => setEditServiceId(null)}
                        disabled={submitting}
                      >
                        Annuler
                      </button>
                    </form>
                  )}

                  <h3 style={{ margin: '0 0 0.75rem', fontSize: '1rem' }}>Services hospitaliers</h3>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Service</th>
                          <th>Lits (total)</th>
                          <th>Chambres</th>
                          <th>Lits enregistrés</th>
                          <th>Statut</th>
                          {isAdmin && <th></th>}
                        </tr>
                      </thead>
                      <tbody>
                        {selectedServices.length === 0 ? (
                          <tr>
                            <td colSpan={isAdmin ? 6 : 5} style={{ color: 'var(--muted)' }}>
                              Aucun service dans ce département.
                              {isAdmin && selectedDept.active
                                ? ' Utilisez « + Service » pour en ajouter un.'
                                : ''}
                            </td>
                          </tr>
                        ) : (
                          selectedServices.map((svc) => {
                            const registered = svc.bedCount ?? 0;
                            const needsBeds = registered < svc.bedCapacity;
                            const beds = bedsByService[svc.id];
                            const occupations = occupationsByService[svc.id];
                            const freeBeds = beds?.filter((b) => !b.occupied).length;
                            return (
                              <Fragment key={svc.id}>
                                <tr>
                                  <td>{svc.name}</td>
                                  <td>{svc.bedCapacity}</td>
                                  <td>
                                    {svc.roomCount ?? '—'}
                                    <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>
                                      {' '}
                                      ({svc.roomLetterPrefix ?? 'A'}1, {svc.roomLetterPrefix ?? 'A'}2…,{' '}
                                      {svc.bedsPerRoom ?? 1} lit
                                      {(svc.bedsPerRoom ?? 1) !== 1 ? 's' : ''}/ch.)
                                    </span>
                                  </td>
                                  <td>
                                    {registered}
                                    {beds && expandedServiceId === svc.id
                                      ? ' (' +
                                        (freeBeds ?? 0) +
                                        ' libre' +
                                        ((freeBeds ?? 0) !== 1 ? 's' : '') +
                                        ')'
                                      : ''}
                                  </td>
                                  <td>{svc.active ? 'Actif' : 'Inactif'}</td>
                                  {isAdmin && (
                                    <td className="catalog-org-service-actions-cell">
                                      <div className="catalog-org-service-actions">
                                        <button
                                          type="button"
                                          className="btn btn-ghost btn-sm"
                                          onClick={() => void toggleServiceBeds(svc)}
                                        >
                                          {expandedServiceId === svc.id ? 'Masquer lits' : 'Voir lits'}
                                        </button>
                                        {needsBeds && (
                                          <button
                                            type="button"
                                            className="btn btn-ghost btn-sm"
                                            onClick={() => void provisionBedsForService(svc)}
                                            disabled={submitting}
                                          >
                                            Générer lits
                                          </button>
                                        )}
                                        {registered > 0 && (
                                          <button
                                            type="button"
                                            className="btn btn-ghost btn-sm"
                                            onClick={() => void realignBedsForService(svc)}
                                            disabled={submitting}
                                            title="Supprime les lits libres et les recrée (A1-01, …)"
                                          >
                                            Recréer lits libres
                                          </button>
                                        )}
                                        <button
                                          type="button"
                                          className="btn btn-ghost btn-sm"
                                          onClick={() => startEditService(svc)}
                                        >
                                          Modifier
                                        </button>
                                        <button
                                          type="button"
                                          className="btn btn-ghost btn-sm"
                                          onClick={() => void toggleService(svc)}
                                          disabled={submitting}
                                        >
                                          {svc.active ? 'Désactiver' : 'Activer'}
                                        </button>
                                        <button
                                          type="button"
                                          className="btn btn-danger btn-sm"
                                          onClick={() => void removeService(svc)}
                                          disabled={submitting}
                                        >
                                          Supprimer
                                        </button>
                                      </div>
                                    </td>
                                  )}
                                </tr>
                                {expandedServiceId === svc.id && (
                                  <tr>
                                    <td colSpan={isAdmin ? 6 : 5} style={{ background: 'var(--surface-elevated, rgba(16,28,61,0.03))' }}>
                                      {!beds ? (
                                        <span style={{ color: 'var(--muted)' }}>Chargement des lits…</span>
                                      ) : beds.length === 0 ? (
                                        <span style={{ color: 'var(--muted)' }}>
                                          Aucun lit enregistré. {isAdmin ? 'Cliquez « Générer lits ».' : ''}
                                        </span>
                                      ) : (
                                        <div>
                                          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                                            {beds.map((b) => (
                                              <span
                                                key={b.id}
                                                style={{
                                                  fontSize: '0.85rem',
                                                  padding: '0.2rem 0.5rem',
                                                  borderRadius: '6px',
                                                  border: '1px solid var(--border)',
                                                  background: b.occupied
                                                    ? 'rgba(220,80,80,0.12)'
                                                    : 'rgba(61,154,237,0.1)',
                                                }}
                                              >
                                                {b.label} {b.occupied ? '· occupé' : '· libre'}
                                              </span>
                                            ))}
                                          </div>
                                          {occupations && occupations.length > 0 ? (
                                            <div style={{ marginTop: '0.85rem' }}>
                                              <p
                                                style={{
                                                  margin: '0 0 0.45rem',
                                                  fontSize: '0.85rem',
                                                  fontWeight: 600,
                                                }}
                                              >
                                                Historique récent des occupations
                                              </p>
                                              <table className="data-table" style={{ fontSize: '0.85rem' }}>
                                                <thead>
                                                  <tr>
                                                    <th scope="col">Lit</th>
                                                    <th scope="col">Patient</th>
                                                    <th scope="col">Admission</th>
                                                    <th scope="col">Début</th>
                                                    <th scope="col">Fin</th>
                                                  </tr>
                                                </thead>
                                                <tbody>
                                                  {occupations.map((row) => (
                                                    <tr key={row.id}>
                                                      <td>{row.bedLabel}</td>
                                                      <td>#{row.patientId}</td>
                                                      <td>#{row.admissionId}</td>
                                                      <td>{formatAuditDateTime(row.startedAt)}</td>
                                                      <td>
                                                        {row.endedAt
                                                          ? formatAuditDateTime(row.endedAt)
                                                          : 'En cours'}
                                                      </td>
                                                    </tr>
                                                  ))}
                                                </tbody>
                                              </table>
                                            </div>
                                          ) : occupations ? (
                                            <p
                                              style={{
                                                margin: '0.75rem 0 0',
                                                fontSize: '0.85rem',
                                                color: 'var(--muted)',
                                              }}
                                            >
                                              Aucune occupation enregistrée pour ce service.
                                            </p>
                                          ) : (
                                            <p
                                              style={{
                                                margin: '0.75rem 0 0',
                                                fontSize: '0.85rem',
                                                color: 'var(--muted)',
                                              }}
                                            >
                                              Chargement de l’historique…
                                            </p>
                                          )}
                                        </div>
                                      )}
                                    </td>
                                  </tr>
                                )}
                              </Fragment>
                            );
                          })
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            )}
          </section>
        </div>
      )}
    </>
  );
}
