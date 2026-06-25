package com.afya.platform.nursing.service;

import com.afya.platform.nursing.dto.AdmissionMedicationAdministrationCreateRequest;
import com.afya.platform.nursing.dto.AdmissionMedicationAdministrationResponse;
import com.afya.platform.nursing.integration.AdmissionServiceClient;
import com.afya.platform.nursing.integration.InternalPrescriptionSummary;
import com.afya.platform.nursing.integration.MedicalServiceClient;
import com.afya.platform.nursing.model.MedicationAdministration;
import com.afya.platform.nursing.repository.MedicationAdministrationRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdmissionMedicationAdministrationService {

    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final MedicalServiceClient medicalServiceClient;
    private final AdmissionServiceClient admissionServiceClient;
    private final PrescriptionNotificationService prescriptionNotificationService;
    private final AuditEventPublisher auditEventPublisher;

    public AdmissionMedicationAdministrationService(
            MedicationAdministrationRepository medicationAdministrationRepository,
            MedicalServiceClient medicalServiceClient,
            AdmissionServiceClient admissionServiceClient,
            PrescriptionNotificationService prescriptionNotificationService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.medicalServiceClient = medicalServiceClient;
        this.admissionServiceClient = admissionServiceClient;
        this.prescriptionNotificationService = prescriptionNotificationService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<AdmissionMedicationAdministrationResponse> listByAdmissionLine(
            Long admissionId,
            Long prescriptionLineId,
            String authHeader
    ) {
        requireAdmissionPrescription(admissionId, prescriptionLineId, authHeader);
        return medicationAdministrationRepository
                .findByPrescriptionLineIdOrderByAdministrationDateDescSlotAsc(prescriptionLineId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdmissionMedicationAdministrationResponse record(
            Long admissionId,
            Long prescriptionLineId,
            AdmissionMedicationAdministrationCreateRequest request,
            String nurseUsername,
            String authHeader
    ) {
        InternalPrescriptionSummary prescription =
                requireAdmissionPrescription(admissionId, prescriptionLineId, authHeader);
        if (!"ACTIVE".equals(prescription.status())) {
            throw new ConflictException("La prescription n'est plus active");
        }
        if (medicationAdministrationRepository
                .findByPrescriptionLineIdAndAdministrationDateAndSlot(
                        prescriptionLineId, request.administrationDate(), request.slot())
                .isPresent()) {
            throw new ConflictException("Une administration existe déjà pour ce créneau");
        }

        MedicationAdministration admin = new MedicationAdministration();
        admin.setPrescriptionLineId(prescriptionLineId);
        admin.setMedicalRecordId(prescription.medicalRecordId());
        admin.setPatientId(prescription.patientId());
        admin.setAdministrationDate(request.administrationDate());
        admin.setSlot(request.slot());
        admin.setAdministered(Boolean.TRUE.equals(request.administered()));
        admin.setNurseUsername(nurseUsername);
        MedicationAdministration saved = medicationAdministrationRepository.save(admin);

        if (saved.isAdministered()) {
            prescriptionNotificationService.markExecuted(prescriptionLineId, saved.getId(), nurseUsername);
        }

        auditEventPublisher.publish(
                "MEDICATION_ADMINISTRATION_RECORDED",
                "MEDICATION_ADMINISTRATION",
                AuditMetadata.resourceId(saved.getId()),
                nurseUsername,
                AuditMetadata.json(
                        "patientId", prescription.patientId(),
                        "prescriptionLineId", prescriptionLineId));

        return toResponse(saved);
    }

    private InternalPrescriptionSummary requireAdmissionPrescription(
            Long admissionId,
            Long prescriptionLineId,
            String authHeader
    ) {
        admissionServiceClient.getAdmission(admissionId, authHeader);
        InternalPrescriptionSummary prescription = medicalServiceClient.prescription(prescriptionLineId, authHeader);
        if (prescription.admissionId() == null || !prescription.admissionId().equals(admissionId)) {
            throw new NotFoundException("Prescription introuvable pour l'admission : " + admissionId);
        }
        return prescription;
    }

    private AdmissionMedicationAdministrationResponse toResponse(MedicationAdministration admin) {
        return new AdmissionMedicationAdministrationResponse(
                admin.getId(),
                admin.getPrescriptionLineId(),
                admin.getAdministrationDate(),
                admin.getSlot(),
                admin.isAdministered());
    }
}
