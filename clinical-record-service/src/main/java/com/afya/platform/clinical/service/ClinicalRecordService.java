package com.afya.platform.clinical.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.clinical.dto.*;
import com.afya.platform.clinical.integration.PatientServiceClient;
import com.afya.platform.clinical.integration.PatientSummary;
import com.afya.platform.clinical.model.*;
import com.afya.platform.clinical.repository.*;
import com.afya.platform.clinical.storage.ClinicalObjectKeyFactory;
import com.afya.platform.clinical.storage.ObjectStorageService;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
public class ClinicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionLineRepository prescriptionLineRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final NursingCareRecordRepository nursingCareRecordRepository;
    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final PatientServiceClient patientServiceClient;
    private final AuditEventPublisher auditEventPublisher;
    private final ObjectStorageService objectStorageService;
    private final long maxUploadBytes;

    public ClinicalRecordService(
            MedicalRecordRepository medicalRecordRepository,
            ClinicalNoteRepository clinicalNoteRepository,
            DiagnosisRepository diagnosisRepository,
            PrescriptionLineRepository prescriptionLineRepository,
            MedicationAdministrationRepository medicationAdministrationRepository,
            NursingCareRecordRepository nursingCareRecordRepository,
            ClinicalDocumentRepository clinicalDocumentRepository,
            PatientServiceClient patientServiceClient,
            AuditEventPublisher auditEventPublisher,
            ObjectStorageService objectStorageService,
            @Value("${app.storage.max-upload-bytes:10485760}") long maxUploadBytes
    ) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.prescriptionLineRepository = prescriptionLineRepository;
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.nursingCareRecordRepository = nursingCareRecordRepository;
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.patientServiceClient = patientServiceClient;
        this.auditEventPublisher = auditEventPublisher;
        this.objectStorageService = objectStorageService;
        this.maxUploadBytes = maxUploadBytes;
    }

    /** Peut créer le dossier à la première consultation (findOrCreateRecord). */
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
        line.setEndDate(null);
        line.setPrescribedBy(username);
        line.setStatus(PrescriptionStatus.ACTIVE);
        PrescriptionLine saved = prescriptionLineRepository.save(line);
        auditEventPublisher.publish(
                "PRESCRIPTION_CREATED", "PRESCRIPTION", AuditMetadata.resourceId(saved.getId()), username, AuditMetadata.patientId(patientId));
        return toPrescriptionResponse(saved);
    }

    public List<PrescriptionLineResponse> listPrescriptions(
            Long patientId,
            Boolean activeOnly,
            String authHeader
    ) {
        MedicalRecord record = ensurePatient(patientId, authHeader, AuditActorResolver.currentUsername());
        List<PrescriptionLine> lines = Boolean.TRUE.equals(activeOnly)
                ? prescriptionLineRepository.findByMedicalRecordIdAndStatusOrderByCreatedAtDesc(
                        record.getId(), PrescriptionStatus.ACTIVE)
                : prescriptionLineRepository.findByMedicalRecordIdOrderByCreatedAtDesc(record.getId());
        return lines.stream().map(this::toPrescriptionResponse).toList();
    }

    @Transactional
    public MedicationAdministrationResponse administer(
            Long prescriptionLineId,
            MedicationAdministrationRequest request,
            String nurseUsername,
            String authHeader
    ) {
        PrescriptionLine line = prescriptionLineRepository.findById(prescriptionLineId)
                .orElseThrow(() -> new NotFoundException("Prescription introuvable : " + prescriptionLineId));
        patientServiceClient.getPatient(line.getMedicalRecord().getPatientId(), authHeader);
        if (line.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new ConflictException("La prescription n'est plus active");
        }
        if (medicationAdministrationRepository.existsByPrescriptionLineId(prescriptionLineId)) {
            throw new ConflictException("Cette prescription a déjà été administrée");
        }
        MedicationAdministration admin = new MedicationAdministration();
        admin.setPrescriptionLine(line);
        admin.setDoseGiven(request.doseGiven());
        admin.setNurseUsername(nurseUsername);
        admin.setNotes(request.notes());
        line.setStatus(PrescriptionStatus.COMPLETED);
        prescriptionLineRepository.save(line);
        MedicationAdministration saved = medicationAdministrationRepository.save(admin);
        Long patientId = line.getMedicalRecord().getPatientId();
        auditEventPublisher.publish(
                "MEDICATION_ADMINISTERED",
                "MEDICATION_ADMINISTRATION",
                AuditMetadata.resourceId(saved.getId()),
                nurseUsername,
                AuditMetadata.json("patientId", patientId, "prescriptionLineId", prescriptionLineId));
        return toAdminResponse(saved);
    }

    @Transactional
    public NursingCareResponse addNursingCare(
            Long patientId,
            NursingCareRequest request,
            String nurseUsername,
            String authHeader
    ) {
        MedicalRecord record = ensurePatient(patientId, authHeader, nurseUsername);
        NursingCareRecord care = new NursingCareRecord();
        care.setMedicalRecord(record);
        care.setCareType(request.careType().strip());
        care.setDescription(request.description().strip());
        care.setNurseUsername(nurseUsername);
        NursingCareRecord saved = nursingCareRecordRepository.save(care);
        auditEventPublisher.publish(
                "NURSING_CARE_RECORDED", "NURSING_CARE", AuditMetadata.resourceId(saved.getId()), nurseUsername, AuditMetadata.patientId(patientId));
        return toNursingResponse(saved);
    }

    public List<NursingCareResponse> listNursingCare(Long patientId, String authHeader) {
        MedicalRecord record = ensurePatient(patientId, authHeader, AuditActorResolver.currentUsername());
        return nursingCareRecordRepository.findByMedicalRecordIdOrderByPerformedAtDesc(record.getId()).stream()
                .map(this::toNursingResponse)
                .toList();
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
                prescriptions.stream().map(this::toPrescriptionResponse).toList(),
                nursingCareRecordRepository.findByMedicalRecordIdOrderByPerformedAtDesc(recordId).stream()
                        .map(this::toNursingResponse).toList(),
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

    private PrescriptionLineResponse toPrescriptionResponse(PrescriptionLine line) {
        boolean administered = medicationAdministrationRepository.existsByPrescriptionLineId(line.getId());
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
                administered
        );
    }

    private MedicationAdministrationResponse toAdminResponse(MedicationAdministration admin) {
        return new MedicationAdministrationResponse(
                admin.getId(),
                admin.getPrescriptionLine().getId(),
                admin.getAdministeredAt(),
                admin.getDoseGiven(),
                admin.getNurseUsername(),
                admin.getNotes()
        );
    }

    private NursingCareResponse toNursingResponse(NursingCareRecord care) {
        return new NursingCareResponse(
                care.getId(),
                care.getCareType(),
                care.getPerformedAt(),
                care.getNurseUsername(),
                care.getDescription()
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
