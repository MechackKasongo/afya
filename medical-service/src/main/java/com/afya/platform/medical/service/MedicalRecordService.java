package com.afya.platform.medical.service;

import com.afya.platform.medical.dto.*;
import com.afya.platform.medical.integration.NursingServiceClient;
import com.afya.platform.medical.integration.PatientServiceClient;
import com.afya.platform.medical.integration.PatientSummary;
import com.afya.platform.medical.model.*;
import com.afya.platform.medical.repository.*;
import com.afya.platform.medical.storage.ClinicalObjectKeyFactory;
import com.afya.platform.medical.storage.ObjectStorageService;
import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MedicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordService.class);

    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionLineRepository prescriptionLineRepository;
    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final PatientServiceClient patientServiceClient;
    private final NursingServiceClient nursingServiceClient;
    private final AuditEventPublisher auditEventPublisher;
    private final ObjectStorageService objectStorageService;
    private final long maxUploadBytes;

    public MedicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            ClinicalNoteRepository clinicalNoteRepository,
            DiagnosisRepository diagnosisRepository,
            PrescriptionLineRepository prescriptionLineRepository,
            ClinicalDocumentRepository clinicalDocumentRepository,
            PatientServiceClient patientServiceClient,
            NursingServiceClient nursingServiceClient,
            AuditEventPublisher auditEventPublisher,
            ObjectStorageService objectStorageService,
            @Value("${app.storage.max-upload-bytes:10485760}") long maxUploadBytes
    ) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.prescriptionLineRepository = prescriptionLineRepository;
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.patientServiceClient = patientServiceClient;
        this.nursingServiceClient = nursingServiceClient;
        this.auditEventPublisher = auditEventPublisher;
        this.objectStorageService = objectStorageService;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Transactional
    public MedicalRecordResponse getMedicalRecord(Long patientId, String authHeader, boolean activePrescriptionsOnly) {
        PatientSummary patient = patientServiceClient.getPatient(patientId, authHeader);
        MedicalRecord record = findOrCreateRecord(patientId, AuditActorResolver.currentUsername());
        return buildFullResponse(record, patient, activePrescriptionsOnly);
    }

    @Transactional
    public MedicalRecordResponse updateAllergies(
            Long patientId,
            MedicalRecordAllergiesUpdateRequest request,
            String username,
            String authHeader
    ) {
        ensurePatient(patientId, authHeader, username);
        MedicalRecord record = findOrCreateRecord(patientId, username);
        record.setAllergies(blankToNull(request.allergies()));
        medicalRecordRepository.save(record);
        auditEventPublisher.publish(
                "MEDICAL_RECORD_ALLERGIES_UPDATED",
                "MEDICAL_RECORD",
                AuditMetadata.resourceId(record.getId()),
                username,
                AuditMetadata.patientId(patientId));
        PatientSummary patient = patientServiceClient.getPatient(patientId, authHeader);
        return buildFullResponse(record, patient, false);
    }

    @Transactional
    public MedicalRecordResponse updateAntecedents(
            Long patientId,
            MedicalRecordAntecedentsUpdateRequest request,
            String username,
            String authHeader
    ) {
        ensurePatient(patientId, authHeader, username);
        MedicalRecord record = findOrCreateRecord(patientId, username);
        record.setAntecedents(blankToNull(request.antecedents()));
        medicalRecordRepository.save(record);
        auditEventPublisher.publish(
                "MEDICAL_RECORD_ANTECEDENTS_UPDATED",
                "MEDICAL_RECORD",
                AuditMetadata.resourceId(record.getId()),
                username,
                AuditMetadata.patientId(patientId));
        PatientSummary patient = patientServiceClient.getPatient(patientId, authHeader);
        return buildFullResponse(record, patient, false);
    }

    @Transactional
    public ClinicalNoteResponse addNote(Long patientId, ClinicalNoteRequest request, String username, String authHeader) {
        MedicalRecord record = ensurePatient(patientId, authHeader, username);
        ClinicalNote note = new ClinicalNote();
        note.setMedicalRecord(record);
        note.setAuthorUsername(username);
        note.setNarrative(request.narrative().strip());
        ClinicalNote saved = clinicalNoteRepository.save(note);
        auditEventPublisher.publish(
                "CLINICAL_NOTE_ADDED", "CLINICAL_NOTE", AuditMetadata.resourceId(saved.getId()), username, AuditMetadata.patientId(patientId));
        return toNoteResponse(saved);
    }

    @Transactional
    public DiagnosisResponse addDiagnosis(Long patientId, DiagnosisRequest request, String username, String authHeader) {
        MedicalRecord record = ensurePatient(patientId, authHeader, username);
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setMedicalRecord(record);
        diagnosis.setCode(request.code());
        diagnosis.setLabel(request.label().strip());
        diagnosis.setAuthorUsername(username);
        Diagnosis saved = diagnosisRepository.save(diagnosis);
        auditEventPublisher.publish(
                "DIAGNOSIS_ADDED", "DIAGNOSIS", AuditMetadata.resourceId(saved.getId()), username, AuditMetadata.patientId(patientId));
        return toDiagnosisResponse(saved);
    }

    @Transactional
    public PrescriptionLineResponse addPrescription(
            Long patientId,
            PrescriptionCreateRequest request,
            String username,
            String authHeader
    ) {
        MedicalRecord record = ensurePatient(patientId, authHeader, username);
        PrescriptionLine line = new PrescriptionLine();
        line.setMedicalRecord(record);
        line.setDrugName(request.drugName().strip());
        line.setDosage(request.prescriptionDetails().strip());
        line.setFrequency("—");
        line.setStartDate(java.time.LocalDate.now());
        line.setPrescribedBy(username);
        line.setStatus(PrescriptionStatus.ACTIVE);
        PrescriptionLine saved = prescriptionLineRepository.save(line);
        auditEventPublisher.publish(
                "PRESCRIPTION_CREATED", "PRESCRIPTION", AuditMetadata.resourceId(saved.getId()), username, AuditMetadata.patientId(patientId));
        notifyNursingPrescriptionCreated(saved.getId(), patientId, saved.getDrugName());
        return toPrescriptionResponse(saved, Set.of());
    }

    private void notifyNursingPrescriptionCreated(Long prescriptionLineId, Long patientId, String drugName) {
        try {
            nursingServiceClient.notifyPrescriptionCreated(prescriptionLineId, patientId, drugName);
        } catch (Exception ex) {
            log.warn("Notification prescription non envoyée au nursing-service (prescriptionLineId={}): {}",
                    prescriptionLineId, ex.getMessage());
        }
    }

    public List<PrescriptionLineResponse> listPrescriptions(Long patientId, Boolean activeOnly, String authHeader) {
        MedicalRecord record = ensurePatient(patientId, authHeader, AuditActorResolver.currentUsername());
        Set<Long> administered = new HashSet<>(nursingServiceClient.administeredPrescriptionLineIds(record.getId()));
        List<PrescriptionLine> lines = Boolean.TRUE.equals(activeOnly)
                ? prescriptionLineRepository.findByMedicalRecordIdAndStatusOrderByCreatedAtDesc(
                        record.getId(), PrescriptionStatus.ACTIVE)
                : prescriptionLineRepository.findByMedicalRecordIdOrderByCreatedAtDesc(record.getId());
        return lines.stream().map(line -> toPrescriptionResponse(line, administered)).toList();
    }

    public InternalMedicalRecordSummary internalMedicalRecordByPatient(Long patientId, String authHeader) {
        patientServiceClient.getPatient(patientId, authHeader);
        MedicalRecord record = findOrCreateRecord(patientId, AuditActorResolver.currentUsername());
        return new InternalMedicalRecordSummary(record.getId(), record.getPatientId());
    }

    public InternalPrescriptionSummary internalPrescription(Long prescriptionLineId, String authHeader) {
        PrescriptionLine line = prescriptionLineRepository.findById(prescriptionLineId)
                .orElseThrow(() -> new NotFoundException("Prescription introuvable : " + prescriptionLineId));
        Long patientId = line.getMedicalRecord().getPatientId();
        patientServiceClient.getPatient(patientId, authHeader);
        return new InternalPrescriptionSummary(
                line.getId(),
                line.getMedicalRecord().getId(),
                patientId,
                line.getAdmissionId(),
                line.getStatus().name(),
                line.getDrugName());
    }

    @Transactional
    public void completePrescription(Long prescriptionLineId) {
        PrescriptionLine line = prescriptionLineRepository.findById(prescriptionLineId)
                .orElseThrow(() -> new NotFoundException("Prescription introuvable : " + prescriptionLineId));
        if (line.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new ConflictException("La prescription n'est plus active");
        }
        line.setStatus(PrescriptionStatus.COMPLETED);
        prescriptionLineRepository.save(line);
    }

    @Transactional
    public ClinicalDocumentResponse addDocument(
            Long patientId,
            ClinicalDocumentRequest request,
            String username,
            String authHeader
    ) {
        MedicalRecord record = ensurePatient(patientId, authHeader, username);
        String objectKey = request.objectStorageKey().strip();
        if (!objectStorageService.exists(objectKey)) {
            throw new BadRequestException("Fichier objet introuvable pour la clé : " + objectKey);
        }
        ClinicalDocument doc = new ClinicalDocument();
        doc.setMedicalRecord(record);
        doc.setTitle(request.title().strip());
        doc.setContentType(request.contentType().strip());
        doc.setObjectStorageKey(objectKey);
        doc.setUploadedBy(username);
        ClinicalDocument saved = clinicalDocumentRepository.save(doc);
        auditEventPublisher.publish(
                "CLINICAL_DOCUMENT_ADDED", "CLINICAL_DOCUMENT", AuditMetadata.resourceId(saved.getId()), username, AuditMetadata.patientId(patientId));
        return toDocumentResponse(saved);
    }

    @Transactional
    public ClinicalDocumentResponse uploadDocument(
            Long patientId,
            String title,
            String contentType,
            String originalFilename,
            InputStream data,
            long size,
            String username,
            String authHeader
    ) {
        if (size <= 0) {
            throw new BadRequestException("Fichier vide");
        }
        if (size > maxUploadBytes) {
            throw new BadRequestException("Fichier trop volumineux (max " + maxUploadBytes + " octets)");
        }
        MedicalRecord record = ensurePatient(patientId, authHeader, username);
        String resolvedTitle = title == null || title.isBlank()
                ? (originalFilename == null || originalFilename.isBlank() ? "Document clinique" : originalFilename.strip())
                : title.strip();
        String resolvedContentType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType.strip();
        String objectKey = ClinicalObjectKeyFactory.forPatientDocument(patientId, originalFilename);
        objectStorageService.put(objectKey, data, size, resolvedContentType);
        ClinicalDocument doc = new ClinicalDocument();
        doc.setMedicalRecord(record);
        doc.setTitle(resolvedTitle);
        doc.setContentType(resolvedContentType);
        doc.setObjectStorageKey(objectKey);
        doc.setUploadedBy(username);
        ClinicalDocument saved = clinicalDocumentRepository.save(doc);
        auditEventPublisher.publish(
                "CLINICAL_DOCUMENT_ADDED",
                "CLINICAL_DOCUMENT",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(patientId));
        return toDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public DownloadedClinicalDocument downloadDocument(Long patientId, Long documentId, String authHeader) {
        patientServiceClient.getPatient(patientId, authHeader);
        ClinicalDocument doc = clinicalDocumentRepository.findByIdAndMedicalRecord_PatientId(documentId, patientId)
                .orElseThrow(() -> new NotFoundException("Document clinique introuvable : " + documentId));
        InputStream stream = objectStorageService.get(doc.getObjectStorageKey());
        return new DownloadedClinicalDocument(stream, doc.getContentType(), doc.getTitle());
    }

    public record DownloadedClinicalDocument(InputStream stream, String contentType, String title) {
    }

    public MedicalRecord ensureRecordForPatient(Long patientId, String username, String authHeader) {
        return ensurePatient(patientId, authHeader, username);
    }

    private MedicalRecord ensurePatient(Long patientId, String authHeader, String actorUsername) {
        patientServiceClient.getPatient(patientId, authHeader);
        return findOrCreateRecord(patientId, actorUsername);
    }

    private MedicalRecord findOrCreateRecord(Long patientId, String actorUsername) {
        return medicalRecordRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    MedicalRecord created = new MedicalRecord();
                    created.setPatientId(patientId);
                    MedicalRecord saved = medicalRecordRepository.save(created);
                    auditEventPublisher.publish(
                            "MEDICAL_RECORD_OPENED",
                            "MEDICAL_RECORD",
                            AuditMetadata.resourceId(saved.getId()),
                            actorUsername,
                            AuditMetadata.patientId(patientId));
                    return saved;
                });
    }

    private MedicalRecordResponse buildFullResponse(
            MedicalRecord record,
            PatientSummary patient,
            boolean activePrescriptionsOnly
    ) {
        Long recordId = record.getId();
        Set<Long> administered = new HashSet<>(nursingServiceClient.administeredPrescriptionLineIds(recordId));
        List<PrescriptionLine> prescriptions = activePrescriptionsOnly
                ? prescriptionLineRepository.findByMedicalRecordIdAndStatusOrderByCreatedAtDesc(
                        recordId, PrescriptionStatus.ACTIVE)
                : prescriptionLineRepository.findByMedicalRecordIdOrderByCreatedAtDesc(recordId);
        return new MedicalRecordResponse(
                recordId,
                patient.id(),
                patient.firstName() + " " + patient.lastName(),
                patient.dossierNumber(),
                record.getOpenedAt(),
                record.getAllergies(),
                record.getAntecedents(),
                clinicalNoteRepository.findByMedicalRecordIdOrderByAuthoredAtDesc(recordId).stream()
                        .map(this::toNoteResponse).toList(),
                diagnosisRepository.findByMedicalRecordIdOrderByRecordedAtDesc(recordId).stream()
                        .map(this::toDiagnosisResponse).toList(),
                prescriptions.stream().map(line -> toPrescriptionResponse(line, administered)).toList(),
                nursingServiceClient.nursingCareByMedicalRecord(recordId),
                clinicalDocumentRepository.findByMedicalRecordIdOrderByUploadedAtDesc(recordId).stream()
                        .map(this::toDocumentResponse).toList()
        );
    }

    private ClinicalNoteResponse toNoteResponse(ClinicalNote note) {
        return new ClinicalNoteResponse(note.getId(), note.getAuthoredAt(), note.getAuthorUsername(), note.getNarrative());
    }

    private DiagnosisResponse toDiagnosisResponse(Diagnosis d) {
        return new DiagnosisResponse(d.getId(), d.getCode(), d.getLabel(), d.getRecordedAt(), d.getAuthorUsername());
    }

    private PrescriptionLineResponse toPrescriptionResponse(PrescriptionLine line, Set<Long> administeredLineIds) {
        return new PrescriptionLineResponse(
                line.getId(),
                line.getDrugName(),
                line.getDosage(),
                line.getFrequency(),
                line.getStartDate(),
                line.getEndDate(),
                line.getStatus().name(),
                line.getPrescribedBy(),
                line.getCreatedAt(),
                administeredLineIds.contains(line.getId())
        );
    }

    private ClinicalDocumentResponse toDocumentResponse(ClinicalDocument doc) {
        return new ClinicalDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getContentType(),
                doc.getObjectStorageKey(),
                doc.getUploadedAt(),
                doc.getUploadedBy()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
