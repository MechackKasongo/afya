package com.afya.platform.nursing.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.nursing.dto.PrescriptionCreateRequest;
import com.afya.platform.nursing.integration.PatientServiceClient;
import com.afya.platform.nursing.integration.PatientSummary;
import com.afya.platform.nursing.model.MedicalRecord;
import com.afya.platform.nursing.model.PrescriptionLine;
import com.afya.platform.nursing.repository.*;
import com.afya.platform.nursing.storage.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NursingCareServiceAuditTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private ClinicalNoteRepository clinicalNoteRepository;
    @Mock
    private DiagnosisRepository diagnosisRepository;
    @Mock
    private PrescriptionLineRepository prescriptionLineRepository;
    @Mock
    private MedicationAdministrationRepository medicationAdministrationRepository;
    @Mock
    private NursingCareRecordRepository nursingCareRecordRepository;
    @Mock
    private ClinicalDocumentRepository clinicalDocumentRepository;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private AuditEventPublisher auditEventPublisher;
    @Mock
    private ObjectStorageService objectStorageService;

    private NursingCareService clinicalRecordService;

    @BeforeEach
    void setUp() {
        clinicalRecordService = new NursingCareService(
                medicalRecordRepository,
                clinicalNoteRepository,
                diagnosisRepository,
                prescriptionLineRepository,
                medicationAdministrationRepository,
                nursingCareRecordRepository,
                clinicalDocumentRepository,
                patientServiceClient,
                auditEventPublisher,
                objectStorageService,
                10_485_760L);
    }

    @Test
    void addPrescriptionPublishesAudit() {
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(1L);
        when(patientServiceClient.getPatient(eq(1L), any()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-1"));
        when(medicalRecordRepository.findByPatientId(1L)).thenReturn(Optional.of(record));
        when(prescriptionLineRepository.save(any(PrescriptionLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PrescriptionCreateRequest request = new PrescriptionCreateRequest(
                "Paracétamol", "500 mg, 3 fois par jour");

        clinicalRecordService.addPrescription(1L, request, "medecin", "Bearer token");

        verify(auditEventPublisher).publish(
                eq("PRESCRIPTION_CREATED"), eq("PRESCRIPTION"), isNull(), eq("medecin"), org.mockito.ArgumentMatchers.contains("patientId"));
    }
}
