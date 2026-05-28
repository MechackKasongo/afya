import { useEffect, useMemo, useState } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { hasAnyRole } from '../auth/roles';
import { api } from '../api/client';
import { platformFeatures } from '../config/features';
import type { AdmissionStayOutletContext } from '../admission/admissionStayContext';
import type { AdmissionResponse, PatientResponse } from '../api/types';
import { PageTabs, type PageTabItem } from './ui/PageTabs';

export type { AdmissionStayOutletContext };

const VITAL_SIGNS_ROLES = ['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER'] as const;
const CLINICAL_FORM_ROLES = ['ROLE_ADMIN', 'ROLE_MEDECIN'] as const;

export function AdmissionStayLayout() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const admissionId = Number(id);
  const base = `/admissions/${id}`;

  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [patientName, setPatientName] = useState('');

  useEffect(() => {
    if (!Number.isFinite(admissionId)) return;
    let cancelled = false;
    void (async () => {
      try {
        const { data: adm } = await api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`);
        if (cancelled) return;
        setAdmission(adm);
        const { data: patient } = await api.get<PatientResponse>(`/api/v1/patients/${adm.patientId}`);
        if (!cancelled) {
          setPatientName(`${patient.firstName} ${patient.lastName}`.trim());
        }
      } catch {
        if (!cancelled) {
          setAdmission(null);
          setPatientName('');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [admissionId]);

  const tabs = useMemo(() => {
    const items: PageTabItem[] = [{ to: base, label: 'Séjour', end: true }];
    if (platformFeatures.admissionClinicalForm && hasAnyRole(user, [...CLINICAL_FORM_ROLES])) {
      items.push({ to: `${base}/clinical-form`, label: 'Saisie clinique' });
    }
    if (platformFeatures.admissionVitalSigns && hasAnyRole(user, [...VITAL_SIGNS_ROLES])) {
      items.push({ to: `${base}/vital-signs`, label: 'Constantes' });
    }
    if (platformFeatures.admissionPrescriptionsByAdmission && hasAnyRole(user, [...CLINICAL_FORM_ROLES])) {
      items.push({ to: `${base}/prescriptions`, label: 'Prescriptions' });
    }
    if (platformFeatures.consultations && admission) {
      items.push({
        to: `/consultations?patientId=${admission.patientId}&admissionId=${admission.id}&open=1`,
        label: 'Consultations',
      });
    }
    return items;
  }, [base, admission, user]);

  return (
    <div className="admission-stay-layout">
      <p className="page-breadcrumb">
        <Link to="/admissions">← Séjours</Link>
        {admission ? (
          <>
            {' · '}
            <Link to={`/patients/${admission.patientId}`}>Fiche patient</Link>
            {' · '}
            <Link to={`/medical-records/${admission.patientId}`}>Dossier médical</Link>
          </>
        ) : null}
      </p>

      <header className="admission-stay-layout__header">
        <div>
          <h1 className="page-title" style={{ marginBottom: 0 }}>
            Séjour {id}
            {patientName ? ` — ${patientName}` : ''}
          </h1>
        </div>
      </header>

      <PageTabs tabs={tabs} ariaLabel="Sections du séjour" />
      <div className="admission-stay-layout__content">
        <Outlet context={{ admission, patientName }} />
      </div>
    </div>
  );
}
