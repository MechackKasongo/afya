package com.afya.platform.nursing.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.nursing.dto.*;
import com.afya.platform.nursing.integration.InternalMedicalRecordSummary;
import com.afya.platform.nursing.integration.InternalPrescriptionSummary;
import com.afya.platform.nursing.integration.MedicalServiceClient;
import com.afya.platform.nursing.model.MedicationAdministration;
import com.afya.platform.nursing.model.NursingCareRecord;
import com.afya.platform.nursing.repository.MedicationAdministrationRepository;
import com.afya.platform.nursing.repository.NursingCareRecordRepository;
import com.afya.platform.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NursingCareService {

    private final NursingCareRecordRepository nursingCareRecordRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final MedicalServiceClient medicalServiceClient;
    private final PrescriptionNotificationService prescriptionNotificationService;
    private final AuditEventPublisher auditEventPublisher;

    public NursingCareService(
            NursingCareRecordRepository nursingCareRecordRepository,
            MedicationAdministrationRepository medicationAdministrationRepository,
            MedicalServiceClient medicalServiceClient,
            PrescriptionNotificationService prescriptionNotificationService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.nursingCareRecordRepository = nursingCareRecordRepository;
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.medicalServiceClient = medicalServiceClient;
        this.prescriptionNotificationService = prescriptionNotificationService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public NursingCareResponse addNursingCare(
            Long patientId,
            NursingCareRequest request,
            String nurseUsername,
            String authHeader
    ) {
        InternalMedicalRecordSummary record = medicalServiceClient.medicalRecordByPatient(patientId, authHeader);
        NursingCareRecord care = new NursingCareRecord();
        care.setMedicalRecordId(record.id());
        care.setCareType(request.careType().strip());
        care.setDescription(request.description().strip());
        care.setNurseUsername(nurseUsername);
        NursingCareRecord saved = nursingCareRecordRepository.save(care);
        auditEventPublisher.publish(
                "NURSING_CARE_RECORDED",
                "NURSING_CARE",
                AuditMetadata.resourceId(saved.getId()),
                nurseUsername,
                AuditMetadata.patientId(patientId));
        return toNursingResponse(saved);
    }

    public List<NursingCareResponse> listNursingCare(Long patientId, String authHeader) {
        InternalMedicalRecordSummary record = medicalServiceClient.medicalRecordByPatient(patientId, authHeader);
        return nursingCareRecordRepository.findByMedicalRecordIdOrderByPerformedAtDesc(record.id()).stream()
                .map(this::toNursingResponse)
                .toList();
    }

    public List<NursingCareResponse> listNursingCareByMedicalRecord(Long medicalRecordId) {
        return nursingCareRecordRepository.findByMedicalRecordIdOrderByPerformedAtDesc(medicalRecordId).stream()
                .map(this::toNursingResponse)
                .toList();
    }

    public List<Long> administeredPrescriptionLineIds(Long medicalRecordId) {
        return medicationAdministrationRepository.findAdministeredLineIdsByMedicalRecordId(medicalRecordId);
    }

    @Transactional
    public MedicationAdministrationResponse administer(
            Long prescriptionLineId,
            MedicationAdministrationRequest request,
            String nurseUsername,
            String authHeader
    ) {
        InternalPrescriptionSummary prescription = medicalServiceClient.prescription(prescriptionLineId, authHeader);
        if (!"ACTIVE".equals(prescription.status())) {
            throw new ConflictException("La prescription n'est plus active");
        }
        if (medicationAdministrationRepository.existsByPrescriptionLineId(prescriptionLineId)) {
            throw new ConflictException("Cette prescription a déjà été administrée");
        }
        MedicationAdministration admin = new MedicationAdministration();
        admin.setPrescriptionLineId(prescriptionLineId);
        admin.setMedicalRecordId(prescription.medicalRecordId());
        admin.setPatientId(prescription.patientId());
        admin.setAdministrationDate(java.time.LocalDate.now());
        admin.setSlot(com.afya.platform.nursing.model.VitalSignSlot.JOURNEE);
        admin.setAdministered(true);
        admin.setDoseGiven(request != null ? request.doseGiven() : null);
        admin.setNurseUsername(nurseUsername);
        admin.setNotes(request != null ? request.notes() : null);
        MedicationAdministration saved = medicationAdministrationRepository.save(admin);
        prescriptionNotificationService.markExecuted(prescriptionLineId, saved.getId(), nurseUsername);
        medicalServiceClient.completePrescription(prescriptionLineId);
        auditEventPublisher.publish(
                "MEDICATION_ADMINISTERED",
                "MEDICATION_ADMINISTRATION",
                AuditMetadata.resourceId(saved.getId()),
                nurseUsername,
                AuditMetadata.json(
                        "patientId", prescription.patientId(),
                        "prescriptionLineId", prescriptionLineId));
        return toAdminResponse(saved);
    }

    private NursingCareResponse toNursingResponse(NursingCareRecord care) {
        return new NursingCareResponse(
                care.getId(),
                care.getCareType(),
                care.getPerformedAt(),
                care.getNurseUsername(),
                care.getDescription());
    }

    private MedicationAdministrationResponse toAdminResponse(MedicationAdministration admin) {
        return new MedicationAdministrationResponse(
                admin.getId(),
                admin.getPrescriptionLineId(),
                admin.getAdministeredAt(),
                admin.getDoseGiven(),
                admin.getNurseUsername(),
                admin.getNotes());
    }
}
