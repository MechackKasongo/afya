import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { RoleRoute } from './components/RoleRoute';
import { AdmissionsPage } from './pages/AdmissionsPage';
import { AdmissionCreatePage } from './pages/AdmissionCreatePage';
import { AdmissionStayLayout } from './components/AdmissionStayLayout';
import { AdmissionDetailPage } from './pages/AdmissionDetailPage';
import { AdmissionVitalSignsPage } from './pages/AdmissionVitalSignsPage';
import { AdmissionPrescriptionsPage } from './pages/AdmissionPrescriptionsPage';
import { ConsultationDetailPage } from './pages/ConsultationDetailPage';
import { ConsultationsPage } from './pages/ConsultationsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { AdmissionClinicalFormPage } from './pages/AdmissionClinicalFormPage';
import { AdmissionClinicalFormReadPage } from './pages/AdmissionClinicalFormReadPage';
import { MedicalRecordDetailPage } from './pages/MedicalRecordDetailPage';
import { MedicalRecordsPage } from './pages/MedicalRecordsPage';
import { MedicationAdministrationsPage } from './pages/MedicationAdministrationsPage';
import { PatientCreatePage } from './pages/PatientCreatePage';
import { PatientDetailPage } from './pages/PatientDetailPage';
import { PatientsPage } from './pages/PatientsPage';
import { ReportingPage } from './pages/ReportingPage';
import { UrgenceDetailPage } from './pages/UrgenceDetailPage';
import { UrgencesPage } from './pages/UrgencesPage';
import { LabRequestsPage } from './pages/LabRequestsPage';
import { LabRequestDetailPage } from './pages/LabRequestDetailPage';
import { UsersPage } from './pages/UsersPage';
import { HospitalServicesPage } from './pages/HospitalServicesPage';
import { SettingsPage } from './pages/SettingsPage';
import { PlatformUnavailablePage } from './components/PlatformUnavailablePage';
import { platformFeatures } from './config/features';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION']} />}>
                <Route path="/patients" element={<PatientsPage />} />
                <Route path="/patients/new" element={<PatientCreatePage />} />
              </Route>
              <Route path="/patients/:id" element={<PatientDetailPage />} />
              <Route path="/admissions" element={<AdmissionsPage />} />
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION', 'ROLE_MEDECIN']} />}>
                <Route path="/admissions/new" element={<AdmissionCreatePage />} />
              </Route>
              <Route path="/admissions/:id" element={<AdmissionStayLayout />}>
                <Route index element={<AdmissionDetailPage />} />
                <Route
                  path="vital-signs"
                  element={
                    <RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']}>
                      <AdmissionVitalSignsPage />
                    </RoleRoute>
                  }
                />
                <Route
                  path="clinical-sheet"
                  element={
                    <RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']}>
                      <AdmissionClinicalFormReadPage />
                    </RoleRoute>
                  }
                />
                <Route
                  path="prescriptions"
                  element={
                    <RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN']}>
                      <AdmissionPrescriptionsPage />
                    </RoleRoute>
                  }
                />
                <Route
                  path="clinical-form"
                  element={
                    <RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN']}>
                      <AdmissionClinicalFormPage />
                    </RoleRoute>
                  }
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route
                  path="/admissions/:admissionId/prescriptions/:lineId/administrations"
                  element={<MedicationAdministrationsPage />}
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route
                  path="/lab/requests"
                  element={
                    platformFeatures.labModule ? (
                      <LabRequestsPage />
                    ) : (
                      <PlatformUnavailablePage
                        title="Laboratoire"
                        description="Le module laboratoire n'est pas encore disponible."
                      />
                    )
                  }
                />
                <Route
                  path="/lab/requests/:id"
                  element={
                    platformFeatures.labModule ? (
                      <LabRequestDetailPage />
                    ) : (
                      <PlatformUnavailablePage
                        title="Demande d'examen"
                        description="Le détail laboratoire n'est pas encore disponible."
                      />
                    )
                  }
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/urgences" element={<UrgencesPage />} />
                <Route path="/urgences/:id" element={<UrgenceDetailPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route
                  path="/consultations"
                  element={
                    platformFeatures.consultations ? (
                      <ConsultationsPage />
                    ) : (
                      <PlatformUnavailablePage
                        title="Consultations"
                        description="Le module consultations n'est pas encore disponible sur la plateforme microservices."
                      />
                    )
                  }
                />
                <Route
                  path="/consultations/:id"
                  element={
                    platformFeatures.consultations ? (
                      <ConsultationDetailPage />
                    ) : (
                      <PlatformUnavailablePage
                        title="Consultation"
                        description="Le détail consultation n'est pas encore disponible sur la plateforme microservices."
                      />
                    )
                  }
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION']} />}>
                <Route path="/hospital-services" element={<HospitalServicesPage />} />
                <Route path="/departments" element={<Navigate to="/hospital-services" replace />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN']} />}>
                <Route path="/reporting" element={<ReportingPage />} />
                <Route
                  path="/users"
                  element={
                    platformFeatures.usersAdmin ? (
                      <UsersPage />
                    ) : (
                      <PlatformUnavailablePage
                        title="Utilisateurs"
                        description="La gestion des utilisateurs reste sur le service identity ; l'interface d'administration arrive prochainement."
                      />
                    )
                  }
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/medical-records" element={<MedicalRecordsPage />} />
                <Route path="/medical-records/:patientId" element={<MedicalRecordDetailPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
