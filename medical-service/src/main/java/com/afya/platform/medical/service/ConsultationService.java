package com.afya.platform.medical.service;

import com.afya.platform.medical.dto.*;
import com.afya.platform.medical.integration.AdmissionSummary;
import com.afya.platform.medical.integration.CareEntryServiceClient;
import com.afya.platform.medical.integration.LabExamRequestCreatePayload;
import com.afya.platform.medical.integration.LabExamRequestSummary;
import com.afya.platform.medical.integration.LabServiceClient;
import com.afya.platform.medical.integration.PatientServiceClient;
import com.afya.platform.medical.model.Consultation;
import com.afya.platform.medical.model.ConsultationEvent;
import com.afya.platform.medical.model.ConsultationEventType;
import com.afya.platform.medical.model.MedicalRecord;
import com.afya.platform.medical.repository.ConsultationEventRepository;
import com.afya.platform.medical.repository.ConsultationRepository;
import com.afya.platform.medical.repository.MedicalRecordRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ConsultationService {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "id", "consultationDateTime", "patientId", "admissionId", "doctorName"
    );

    private final ConsultationRepository consultationRepository;
    private final ConsultationEventRepository consultationEventRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientServiceClient patientServiceClient;
    private final CareEntryServiceClient careEntryServiceClient;
    private final LabServiceClient labServiceClient;
    private final AuditEventPublisher auditEventPublisher;
    private final DiseaseCatalogService diseaseCatalogService;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            ConsultationEventRepository consultationEventRepository,
            MedicalRecordRepository medicalRecordRepository,
            PatientServiceClient patientServiceClient,
            CareEntryServiceClient careEntryServiceClient,
            LabServiceClient labServiceClient,
            AuditEventPublisher auditEventPublisher,
            DiseaseCatalogService diseaseCatalogService
    ) {
        this.consultationRepository = consultationRepository;
        this.consultationEventRepository = consultationEventRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientServiceClient = patientServiceClient;
        this.careEntryServiceClient = careEntryServiceClient;
        this.labServiceClient = labServiceClient;
        this.auditEventPublisher = auditEventPublisher;
        this.diseaseCatalogService = diseaseCatalogService;
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> list(
            Long patientId,
            Long admissionId,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir
    ) {
        PageRequest pageable = buildPageRequest(page, size, sortBy, sortDir);
        return consultationRepository.search(patientId, admissionId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getById(Long consultationId) {
        return toResponse(findConsultation(consultationId));
    }

    @Transactional
    public ConsultationResponse create(ConsultationCreateRequest request, String username, String authHeader) {
        patientServiceClient.getPatient(request.patientId(), authHeader);
        AdmissionSummary admission = careEntryServiceClient.getAdmission(request.admissionId(), authHeader);
        if (!request.patientId().equals(admission.patientId())) {
            throw new BadRequestException(
                    "L'admission #" + request.admissionId() + " n'appartient pas au patient #" + request.patientId());
        }
        ensureMedicalRecord(request.patientId());

        Consultation consultation = new Consultation();
        consultation.setPatientId(request.patientId());
        consultation.setAdmissionId(request.admissionId());
        consultation.setDoctorName(request.doctorName().strip());
        consultation.setReason(blankToNull(request.reason()));
        Consultation saved = consultationRepository.save(consultation);
        auditEventPublisher.publish(
                "CONSULTATION_CREATED",
                "CONSULTATION",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(request.patientId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsultationEventResponse> patientTimeline(Long patientId) {
        return consultationEventRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationEventResponse> consultationEvents(Long consultationId) {
        findConsultation(consultationId);
        return consultationEventRepository.findByConsultation_IdOrderByCreatedAtDesc(consultationId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional
    public ConsultationEventResponse addObservation(
            Long consultationId,
            EventCreateRequest request,
            String username
    ) {
        return addEvent(consultationId, ConsultationEventType.OBSERVATION, request, username, "CONSULTATION_OBSERVATION_ADDED");
    }

    @Transactional
    public ConsultationEventResponse addDiagnostic(
            Long consultationId,
            EventCreateRequest request,
            String username
    ) {
        return addEvent(consultationId, ConsultationEventType.DIAGNOSTIC, request, username, "CONSULTATION_DIAGNOSTIC_ADDED");
    }

    @Transactional
    public ConsultationEventResponse addExamOrder(
            Long consultationId,
            ExamOrderCreateRequest request,
            String username,
            String authHeader
    ) {
        Consultation consultation = findConsultation(consultationId);
        String content = blankToNull(request.content());
        if (content == null) {
            content = "Demande d'examen depuis la consultation";
        }
        ExamUrgency urgency = request.urgency() != null ? request.urgency() : ExamUrgency.NORMAL;

        LabExamRequestSummary labRequest = labServiceClient.createExamRequest(
                new LabExamRequestCreatePayload(
                        consultation.getPatientId(),
                        request.doctorId(),
                        consultation.getAdmissionId(),
                        urgency.name(),
                        content,
                        request.examTypeIds()),
                authHeader);
        if (labRequest == null || labRequest.id() == null) {
            throw new BadRequestException("Création de la demande labo impossible");
        }

        ConsultationEvent event = new ConsultationEvent();
        event.setConsultation(consultation);
        event.setPatientId(consultation.getPatientId());
        event.setEventType(ConsultationEventType.EXAM_ORDER);
        event.setContent(content);
        event.setExamRequestId(labRequest.id());
        ConsultationEvent saved = consultationEventRepository.save(event);
        auditEventPublisher.publish(
                "CONSULTATION_EXAM_ORDER_ADDED",
                "CONSULTATION_EVENT",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.json(
                        "patientId", consultation.getPatientId(),
                        "examRequestId", labRequest.id()));
        return toEventResponse(saved);
    }

    /**
     * M1 — « transmettre les résultats » : appelé en interne par le lab-service lorsqu'un compte rendu
     * est publié. Ajoute un événement RESULT_AVAILABLE à la consultation à l'origine de la demande
     * (rattachée via examRequestId), de façon idempotente. Sans EXAM_ORDER correspondant : sans effet.
     */
    @Transactional
    public void markExamResultAvailable(Long examRequestId) {
        if (examRequestId == null) {
            return;
        }
        if (consultationEventRepository.existsByExamRequestIdAndEventType(
                examRequestId, ConsultationEventType.RESULT_AVAILABLE)) {
            return;
        }
        ConsultationEvent order = consultationEventRepository
                .findFirstByExamRequestIdAndEventType(examRequestId, ConsultationEventType.EXAM_ORDER)
                .orElse(null);
        if (order == null) {
            return;
        }
        ConsultationEvent event = new ConsultationEvent();
        event.setConsultation(order.getConsultation());
        event.setPatientId(order.getPatientId());
        event.setEventType(ConsultationEventType.RESULT_AVAILABLE);
        event.setContent("Résultats de laboratoire disponibles pour la demande n° " + examRequestId);
        event.setExamRequestId(examRequestId);
        ConsultationEvent saved = consultationEventRepository.save(event);
        auditEventPublisher.publish(
                "CONSULTATION_EXAM_RESULT_AVAILABLE",
                "CONSULTATION_EVENT",
                AuditMetadata.resourceId(saved.getId()),
                "lab-service",
                AuditMetadata.json(
                        "patientId", order.getPatientId(),
                        "examRequestId", examRequestId));
    }

    private ConsultationEventResponse addEvent(
            Long consultationId,
            ConsultationEventType type,
            EventCreateRequest request,
            String username,
            String auditAction
    ) {
        Consultation consultation = findConsultation(consultationId);
        ConsultationEvent event = new ConsultationEvent();
        event.setConsultation(consultation);
        event.setPatientId(consultation.getPatientId());
        event.setEventType(type);
        event.setContent(request.content().strip());
        if (type == ConsultationEventType.DIAGNOSTIC) {
            String diseaseType = blankToNull(request.diseaseType());
            String diseaseName = blankToNull(request.diseaseName());
            if (diseaseType == null || diseaseName == null) {
                throw new BadRequestException("Le type de maladie et le nom de la maladie sont obligatoires pour un diagnostic.");
            }
            event.setDiseaseType(diseaseType);
            event.setDiseaseName(diseaseName);
            diseaseCatalogService.recordUsage(diseaseType, diseaseName);
        } else {
            event.setDiseaseType(null);
            event.setDiseaseName(null);
        }
        ConsultationEvent saved = consultationEventRepository.save(event);
        auditEventPublisher.publish(
                auditAction,
                "CONSULTATION_EVENT",
                AuditMetadata.resourceId(saved.getId()),
                username,
                AuditMetadata.patientId(consultation.getPatientId()));
        return toEventResponse(saved);
    }

    private Consultation findConsultation(Long consultationId) {
        return consultationRepository.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("Consultation introuvable : " + consultationId));
    }

    private void ensureMedicalRecord(Long patientId) {
        medicalRecordRepository.findByPatientId(patientId).orElseGet(() -> {
            MedicalRecord record = new MedicalRecord();
            record.setPatientId(patientId);
            return medicalRecordRepository.save(record);
        });
    }

    private static PageRequest buildPageRequest(Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? 20 : Math.min(size, 500);
        String property = sortBy == null || sortBy.isBlank() || !ALLOWED_SORT.contains(sortBy)
                ? "consultationDateTime"
                : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ConsultationResponse toResponse(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getId(),
                consultation.getPatientId(),
                consultation.getAdmissionId(),
                consultation.getDoctorName(),
                consultation.getReason(),
                consultation.getConsultationDateTime()
        );
    }

    private ConsultationEventResponse toEventResponse(ConsultationEvent event) {
        return new ConsultationEventResponse(
                event.getId(),
                event.getConsultation().getId(),
                event.getPatientId(),
                event.getEventType().name(),
                event.getContent(),
                event.getDiseaseType(),
                event.getDiseaseName(),
                event.getExamRequestId(),
                event.getCreatedAt()
        );
    }
}
