package com.afya.platform.medical.service;

import com.afya.platform.medical.dto.AdmissionPrescriptionLineCreateRequest;
import com.afya.platform.medical.dto.AdmissionPrescriptionLineResponse;
import com.afya.platform.medical.dto.AdmissionPrescriptionLineUpdateRequest;
import com.afya.platform.medical.integration.AdmissionSummary;
import com.afya.platform.medical.integration.CareEntryServiceClient;
import com.afya.platform.medical.integration.NursingServiceClient;
import com.afya.platform.medical.model.MedicalRecord;
import com.afya.platform.medical.model.PrescriptionLine;
import com.afya.platform.medical.model.PrescriptionStatus;
import com.afya.platform.medical.repository.PrescriptionLineRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdmissionPrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionPrescriptionService.class);

    private final PrescriptionLineRepository prescriptionLineRepository;
    private final MedicalRecordService medicalRecordService;
    private final CareEntryServiceClient careEntryServiceClient;
    private final NursingServiceClient nursingServiceClient;
    private final AuditEventPublisher auditEventPublisher;

    public AdmissionPrescriptionService(
            PrescriptionLineRepository prescriptionLineRepository,
            MedicalRecordService medicalRecordService,
            CareEntryServiceClient careEntryServiceClient,
            NursingServiceClient nursingServiceClient,
            AuditEventPublisher auditEventPublisher
    ) {
        this.prescriptionLineRepository = prescriptionLineRepository;
        this.medicalRecordService = medicalRecordService;
        this.careEntryServiceClient = careEntryServiceClient;
        this.nursingServiceClient = nursingServiceClient;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<AdmissionPrescriptionLineResponse> listByAdmission(Long admissionId, String authHeader) {
        careEntryServiceClient.getAdmission(admissionId, authHeader);
        return prescriptionLineRepository.findByAdmissionIdOrderByCreatedAtDesc(admissionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdmissionPrescriptionLineResponse create(
            Long admissionId,
            AdmissionPrescriptionLineCreateRequest request,
            String username,
            String authHeader
    ) {
        AdmissionSummary admission = careEntryServiceClient.getAdmission(admissionId, authHeader);
        MedicalRecord record = medicalRecordService.ensureRecordForPatient(admission.patientId(), username, authHeader);
        PrescriptionLine line = new PrescriptionLine();
        line.setMedicalRecord(record);
        line.setAdmissionId(admissionId);
        applyCreateFields(line, request, username);
        line.setStatus(PrescriptionStatus.ACTIVE);
        PrescriptionLine saved = prescriptionLineRepository.save(line);
        auditEventPublisher.publish(
                "PRESCRIPTION_CREATED",
                "PRESCRIPTION",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(admission.patientId()));
        notifyNursingPrescriptionCreated(saved.getId(), admission.patientId(), saved.getDrugName());
        return toResponse(saved);
    }

    @Transactional
    public AdmissionPrescriptionLineResponse update(
            Long admissionId,
            Long lineId,
            AdmissionPrescriptionLineUpdateRequest request,
            String username,
            String authHeader
    ) {
        careEntryServiceClient.getAdmission(admissionId, authHeader);
        PrescriptionLine line = requireAdmissionLine(admissionId, lineId);
        applyUpdateFields(line, request, username);
        PrescriptionLine saved = prescriptionLineRepository.save(line);
        auditEventPublisher.publish(
                "PRESCRIPTION_UPDATED",
                "PRESCRIPTION",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(line.getMedicalRecord().getPatientId()));
        return toResponse(saved);
    }

    PrescriptionLine requireAdmissionLine(Long admissionId, Long lineId) {
        PrescriptionLine line = prescriptionLineRepository.findById(lineId)
                .orElseThrow(() -> new NotFoundException("Prescription introuvable : " + lineId));
        if (line.getAdmissionId() == null || !line.getAdmissionId().equals(admissionId)) {
            throw new NotFoundException("Prescription introuvable pour l'admission : " + admissionId);
        }
        return line;
    }

    private void applyCreateFields(
            PrescriptionLine line,
            AdmissionPrescriptionLineCreateRequest request,
            String username
    ) {
        line.setDrugName(request.medicationName().strip());
        line.setDosage(resolveDosage(
                request.dosageText(),
                request.instructionsText()));
        line.setFrequency(blankToDash(request.frequencyText()));
        line.setStartDate(request.startDate());
        line.setEndDate(request.endDate());
        line.setPrescribedBy(resolvePrescriber(request.prescriberName(), username));
    }

    private void applyUpdateFields(
            PrescriptionLine line,
            AdmissionPrescriptionLineUpdateRequest request,
            String username
    ) {
        if (line.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new ConflictException("Une prescription administrée ne peut plus être modifiée");
        }
        line.setDrugName(request.medicationName().strip());
        line.setDosage(resolveDosage(
                request.dosageText(),
                request.instructionsText()));
        line.setFrequency(blankToDash(request.frequencyText()));
        line.setStartDate(request.startDate());
        line.setEndDate(request.endDate());
        line.setPrescribedBy(resolvePrescriber(request.prescriberName(), username));
        line.setStatus(Boolean.TRUE.equals(request.active()) ? PrescriptionStatus.ACTIVE : PrescriptionStatus.CANCELLED);
    }

    private static String resolveDosage(String dosageText, String instructionsText) {
        String dosage = blankToNull(dosageText);
        if (dosage == null) {
            dosage = blankToNull(instructionsText);
        }
        if (dosage == null) {
            throw new ConflictException("Les détails de la prescription sont obligatoires");
        }
        return dosage;
    }

    private static String resolvePrescriber(String prescriberName, String username) {
        String name = blankToNull(prescriberName);
        return name != null ? name : username;
    }

    private AdmissionPrescriptionLineResponse toResponse(PrescriptionLine line) {
        return new AdmissionPrescriptionLineResponse(
                line.getId(),
                line.getAdmissionId(),
                line.getDrugName(),
                line.getDosage(),
                dashToNull(line.getFrequency()),
                null,
                line.getPrescribedBy(),
                line.getStartDate(),
                line.getEndDate(),
                line.getStatus() == PrescriptionStatus.ACTIVE,
                line.getCreatedAt());
    }

    private void notifyNursingPrescriptionCreated(Long prescriptionLineId, Long patientId, String drugName) {
        try {
            nursingServiceClient.notifyPrescriptionCreated(prescriptionLineId, patientId, drugName);
        } catch (Exception ex) {
            log.warn("Notification prescription non envoyée au nursing-service (prescriptionLineId={}): {}",
                    prescriptionLineId, ex.getMessage());
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static String blankToDash(String value) {
        String normalized = blankToNull(value);
        return normalized != null ? normalized : "—";
    }

    private static String dashToNull(String value) {
        if (value == null || value.isBlank() || "—".equals(value.strip())) {
            return null;
        }
        return value.strip();
    }
}
