package com.afya.platform.admission.service;

import com.afya.platform.admission.dto.EmergencyCreateRequest;
import com.afya.platform.admission.dto.EmergencyOrientationRequest;
import com.afya.platform.admission.dto.EmergencyResponse;
import com.afya.platform.admission.dto.EmergencyTriageRequest;
import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.model.EmergencyStatus;
import com.afya.platform.admission.model.EmergencyVisit;
import com.afya.platform.admission.repository.EmergencyVisitRepository;
import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class EmergencyVisitService {

    private final EmergencyVisitRepository emergencyVisitRepository;
    private final PatientServiceClient patientServiceClient;
    private final AuditEventPublisher auditEventPublisher;
    private final EmergencyVisitTimelineService timelineService;

    public EmergencyVisitService(
            EmergencyVisitRepository emergencyVisitRepository,
            PatientServiceClient patientServiceClient,
            AuditEventPublisher auditEventPublisher,
            EmergencyVisitTimelineService timelineService
    ) {
        this.emergencyVisitRepository = emergencyVisitRepository;
        this.patientServiceClient = patientServiceClient;
        this.auditEventPublisher = auditEventPublisher;
        this.timelineService = timelineService;
    }

    @Transactional
    public EmergencyResponse create(EmergencyCreateRequest request, String authorizationHeader) {
        PatientSummary patient = patientServiceClient.getPatient(request.patientId(), authorizationHeader);
        EmergencyVisit visit = new EmergencyVisit();
        visit.setPatientId(request.patientId());
        visit.setArrivedAt(request.arrivedAt() != null ? request.arrivedAt() : Instant.now());
        visit.setTriageNotes(request.triageNotes());
        visit.setPriority(normalizePriority(request.priority()));
        visit.setStatus(EmergencyStatus.EN_ATTENTE_TRIAGE);
        EmergencyVisit saved = emergencyVisitRepository.save(visit);
        timelineService.record(
                saved.getId(),
                "ARRIVEE",
                formatArrivalDetails(saved.getPriority(), saved.getTriageNotes()));
        auditEventPublisher.publish(
                "EMERGENCY_VISIT_CREATED",
                "EMERGENCY_VISIT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        return toResponse(saved, patient);
    }

    public EmergencyResponse getById(Long id, String authorizationHeader) {
        EmergencyVisit visit = find(id);
        PatientSummary patient = patientServiceClient.getPatient(visit.getPatientId(), authorizationHeader);
        return toResponse(visit, patient);
    }

    public Page<EmergencyResponse> list(
            String status,
            String priority,
            String sortBy,
            String sortDir,
            int page,
            int size,
            String authorizationHeader
    ) {
        Specification<EmergencyVisit> spec = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            EmergencyStatus parsed = parseStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (priority != null && !priority.isBlank()) {
            String normalized = priority.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), normalized));
        }
        Sort sort = resolveSort(sortBy, sortDir);
        Page<EmergencyVisit> visits = emergencyVisitRepository.findAll(spec, PageRequest.of(page, size, sort));
        return visits.map(v -> {
            PatientSummary patient = patientServiceClient.getPatient(v.getPatientId(), authorizationHeader);
            return toResponse(v, patient);
        });
    }

    @Transactional
    public EmergencyResponse triage(Long id, EmergencyTriageRequest request, String authorizationHeader) {
        EmergencyVisit visit = find(id);
        if (visit.getStatus() == EmergencyStatus.CLOTURE) {
            throw new BadRequestException("Le passage aux urgences est clôturé");
        }
        visit.setTriageLevel(request.triageLevel().trim());
        if (request.details() != null && !request.details().isBlank()) {
            String existing = visit.getTriageNotes();
            String appended = request.details().trim();
            visit.setTriageNotes(existing == null || existing.isBlank() ? appended : existing + " | " + appended);
        }
        if (visit.getStatus() == EmergencyStatus.EN_ATTENTE_TRIAGE) {
            visit.setStatus(EmergencyStatus.EN_COURS);
        }
        EmergencyVisit saved = emergencyVisitRepository.save(visit);
        timelineService.record(
                saved.getId(),
                "TRIAGE",
                formatTriageDetails(request.triageLevel().trim(), request.details()));
        auditEventPublisher.publish(
                "EMERGENCY_TRIAGE_RECORDED",
                "EMERGENCY_VISIT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        PatientSummary patient = patientServiceClient.getPatient(saved.getPatientId(), authorizationHeader);
        return toResponse(saved, patient);
    }

    @Transactional
    public EmergencyResponse orient(Long id, EmergencyOrientationRequest request, String authorizationHeader) {
        EmergencyVisit visit = find(id);
        if (visit.getStatus() == EmergencyStatus.CLOTURE) {
            throw new BadRequestException("Le passage aux urgences est clôturé");
        }
        String transferTarget = request.orientation().trim();
        visit.setOrientation(transferTarget);
        visit.setStatus(EmergencyStatus.CLOTURE);
        visit.setEndedAt(Instant.now());
        EmergencyVisit saved = emergencyVisitRepository.save(visit);
        timelineService.record(saved.getId(), "ORIENTATION", transferTarget);
        timelineService.record(saved.getId(), "CLOTURE", "Passage clôturé automatiquement après orientation");
        auditEventPublisher.publish(
                "EMERGENCY_ORIENTED",
                "EMERGENCY_VISIT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        auditEventPublisher.publish(
                "EMERGENCY_VISIT_CLOSED",
                "EMERGENCY_VISIT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        PatientSummary patient = patientServiceClient.getPatient(saved.getPatientId(), authorizationHeader);
        return toResponse(saved, patient);
    }

    @Transactional
    public EmergencyResponse close(Long id, String authorizationHeader) {
        EmergencyVisit visit = find(id);
        if (visit.getStatus() == EmergencyStatus.CLOTURE) {
            throw new BadRequestException("Le passage aux urgences est déjà clôturé");
        }
        visit.setStatus(EmergencyStatus.CLOTURE);
        visit.setEndedAt(Instant.now());
        EmergencyVisit saved = emergencyVisitRepository.save(visit);
        timelineService.record(saved.getId(), "CLOTURE", "Passage clôturé");
        auditEventPublisher.publish(
                "EMERGENCY_VISIT_CLOSED",
                "EMERGENCY_VISIT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        PatientSummary patient = patientServiceClient.getPatient(saved.getPatientId(), authorizationHeader);
        return toResponse(saved, patient);
    }

    private EmergencyVisit find(Long id) {
        return emergencyVisitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Passage urgences introuvable : " + id));
    }

    private static EmergencyResponse toResponse(EmergencyVisit visit, PatientSummary patient) {
        return new EmergencyResponse(
                visit.getId(),
                visit.getPatientId(),
                patient.firstName() + " " + patient.lastName(),
                visit.getArrivedAt(),
                visit.getEndedAt(),
                visit.getStatus().name(),
                visit.getTriageNotes(),
                visit.getPriority(),
                visit.getTriageLevel(),
                visit.getOrientation()
        );
    }

    private static String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "P2";
        }
        return priority.trim().toUpperCase(Locale.ROOT);
    }

    private static EmergencyStatus parseStatus(String status) {
        try {
            return EmergencyStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut urgences invalide : " + status);
        }
    }

    private static String formatArrivalDetails(String priority, String motif) {
        StringBuilder sb = new StringBuilder("Priorité ").append(priority);
        if (motif != null && !motif.isBlank()) {
            sb.append(" — Motif : ").append(motif.strip());
        }
        return sb.toString();
    }

    private static String formatTriageDetails(String triageLevel, String details) {
        StringBuilder sb = new StringBuilder("Niveau ").append(triageLevel);
        if (details != null && !details.isBlank()) {
            sb.append(" — ").append(details.strip());
        }
        return sb.toString();
    }

    private static Sort resolveSort(String sortBy, String sortDir) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "createdAt" -> "arrivedAt";
            case "patientId" -> "patientId";
            case "priority" -> "priority";
            case "status" -> "status";
            default -> "id";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
