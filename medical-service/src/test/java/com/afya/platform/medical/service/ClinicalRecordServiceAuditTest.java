package com.afya.platform.medical.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.medical.dto.PrescriptionCreateRequest;
import com.afya.platform.medical.integration.NursingServiceClient;
import com.afya.platform.medical.integration.PatientServiceClient;
import com.afya.platform.medical.integration.PatientSummary;
import com.afya.platform.medical.model.MedicalRecord;
import com.afya.platform.medical.model.PrescriptionLine;
import com.afya.platform.medical.repository.*;
import com.afya.platform.medical.storage.ObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceAuditTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private ClinicalNoteRepository clinicalNoteRepository;
    @Mock
    private DiagnosisRepository diagnosisRepository;
    @Mock
    private PrescriptionLineRepository prescriptionLineRepository;
    @Mock
    private ClinicalDocumentRepository clinicalDocumentRepository;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private NursingServiceClient nursingServiceClient;
    @Mock
    private AuditEventPublisher auditEventPublisher;
    @Mock
    private ObjectStorageService objectStorageService;

    private MedicalRecordService clinicalRecordService;

    @BeforeEach
    void setUp() {
        clinicalRecordService = new MedicalRecordService(
                medicalRecordRepository,
                clinicalNoteRepository,
                diagnosisRepository,
                prescriptionLineRepository,
                clinicalDocumentRepository,
                patientServiceClient,
                nursingServiceClient,
                auditEventPublisher,
                objectStorageService,
                10_485_760L);
    }

    @Test
    void addPrescriptionPublishesAudit() throws Exception {
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(1L);
        when(patientServiceClient.getPatient(eq(1L), any()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-1"));
        when(medicalRecordRepository.findByPatientId(1L)).thenReturn(Optional.of(record));
        when(prescriptionLineRepository.save(any(PrescriptionLine.class)))
                .thenAnswer(invocation -> {
                    PrescriptionLine line = invocation.getArgument(0);
                    Field idField = PrescriptionLine.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(line, 42L);
                    return line;
                });

        PrescriptionCreateRequest request = new PrescriptionCreateRequest(
                "Paracétamol", "500 mg, 3 fois par jour");

        clinicalRecordService.addPrescription(1L, request, "medecin", "Bearer token");

        verify(auditEventPublisher).publish(
                eq("PRESCRIPTION_CREATED"), eq("PRESCRIPTION"), eq("42"), eq("medecin"), org.mockito.ArgumentMatchers.contains("patientId"));
        verify(nursingServiceClient).notifyPrescriptionCreated(42L, 1L, "Paracétamol");
    }
}
