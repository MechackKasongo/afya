package com.afya.platform.lab.service;

import com.afya.platform.lab.dto.ExamRequestCreateRequest;
import com.afya.platform.lab.dto.ExamRequestResponse;
import com.afya.platform.lab.dto.ExamResultRequest;
import com.afya.platform.lab.dto.ExamResultResponse;
import com.afya.platform.lab.dto.ExamTypeRequest;
import com.afya.platform.lab.dto.ExamTypeResponse;
import com.afya.platform.lab.dto.ExamTypeSummary;
import com.afya.platform.lab.dto.ResultParameterResponse;
import com.afya.platform.lab.dto.SpecimenCollectionRequest;
import com.afya.platform.lab.model.ExamRequest;
import com.afya.platform.lab.model.ExamRequestLine;
import com.afya.platform.lab.model.ExamRequestStatus;
import com.afya.platform.lab.model.ExamResult;
import com.afya.platform.lab.model.ExamType;
import com.afya.platform.lab.model.ResultParameter;
import com.afya.platform.lab.model.SpecimenCollection;
import com.afya.platform.lab.repository.ExamRequestRepository;
import com.afya.platform.lab.repository.ExamResultRepository;
import com.afya.platform.lab.repository.ExamTypeRepository;
import com.afya.platform.lab.repository.SpecimenCollectionRepository;
import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LabService {

    private final ExamTypeRepository examTypeRepository;
    private final ExamRequestRepository examRequestRepository;
    private final SpecimenCollectionRepository specimenCollectionRepository;
    private final ExamResultRepository examResultRepository;
    private final AuditEventPublisher auditEventPublisher;

    public LabService(
            ExamTypeRepository examTypeRepository,
            ExamRequestRepository examRequestRepository,
            SpecimenCollectionRepository specimenCollectionRepository,
            ExamResultRepository examResultRepository,
            AuditEventPublisher auditEventPublisher) {
        this.examTypeRepository = examTypeRepository;
        this.examRequestRepository = examRequestRepository;
        this.specimenCollectionRepository = specimenCollectionRepository;
        this.examResultRepository = examResultRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ExamTypeResponse> listActiveExamTypes() {
        return examTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toTypeResponse)
                .toList();
    }

    @Transactional
    public ExamTypeResponse createExamType(ExamTypeRequest request) {
        String name = request.name().strip();
        if (examTypeRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("Type d'examen déjà défini : " + name);
        }
        ExamType type = new ExamType();
        type.setName(name);
        type.setDescription(blankToNull(request.description()));
        type.setCategory(request.category());
        type.setParameters(blankToNull(request.parameters()));
        ExamType saved = examTypeRepository.save(type);
        publish("EXAM_TYPE_CREATED", "ExamType", saved.getId());
        return toTypeResponse(saved);
    }

    @Transactional
    public ExamRequestResponse createRequest(ExamRequestCreateRequest request) {
        ExamRequest entity = new ExamRequest();
        entity.setPatientId(request.patientId());
        entity.setDoctorId(request.doctorId());
        entity.setAdmissionId(request.admissionId());
        entity.setUrgency(request.urgency());
        entity.setComment(blankToNull(request.comment()));
        for (Long typeId : request.examTypeIds()) {
            ExamType type = examTypeRepository.findById(typeId)
                    .orElseThrow(() -> new NotFoundException("Type d'examen introuvable : " + typeId));
            if (!type.isActive()) {
                throw new BadRequestException("Type d'examen inactif : " + type.getName());
            }
            ExamRequestLine line = new ExamRequestLine();
            line.setExamType(type);
            entity.addLine(line);
        }
        ExamRequest saved = examRequestRepository.save(entity);
        publish("EXAM_REQUEST_CREATED", "ExamRequest", saved.getId());
        return toRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExamRequestResponse> listRequests(ExamRequestStatus status, Pageable pageable) {
        Page<ExamRequest> page = status == null
                ? examRequestRepository.findAll(pageable)
                : examRequestRepository.findByStatus(status, pageable);
        return page.map(this::toRequestResponse);
    }

    @Transactional(readOnly = true)
    public ExamRequestResponse getRequest(Long id) {
        return toRequestResponse(findRequest(id));
    }

    @Transactional
    public ExamRequestResponse recordSpecimen(Long requestId, SpecimenCollectionRequest request) {
        ExamRequest examRequest = findRequest(requestId);
        if (examRequest.getStatus() != ExamRequestStatus.PENDING) {
            throw new BadRequestException("Prélèvement impossible pour le statut : " + examRequest.getStatus());
        }
        SpecimenCollection collection = new SpecimenCollection();
        collection.setRequest(examRequest);
        collection.setLabTechnicianId(request.labTechnicianId());
        collection.setSampleType(request.sampleType().strip());
        specimenCollectionRepository.save(collection);
        examRequest.setStatus(ExamRequestStatus.SPECIMEN_COLLECTED);
        publish("EXAM_SPECIMEN_COLLECTED", "ExamRequest", requestId);
        return toRequestResponse(examRequest);
    }

    @Transactional
    public ExamResultResponse recordResult(Long requestId, ExamResultRequest request) {
        ExamRequest examRequest = findRequest(requestId);
        if (examRequest.getStatus() != ExamRequestStatus.SPECIMEN_COLLECTED) {
            throw new BadRequestException("Résultat impossible pour le statut : " + examRequest.getStatus());
        }
        if (examResultRepository.findByRequestId(requestId).isPresent()) {
            throw new ConflictException("Résultat déjà enregistré pour la demande " + requestId);
        }
        ExamResult result = new ExamResult();
        result.setRequest(examRequest);
        result.setPatientId(examRequest.getPatientId());
        result.setLabTechnicianId(request.labTechnicianId());
        result.setAnnotation(blankToNull(request.annotation()));
        for (var param : request.parameters()) {
            ResultParameter rp = new ResultParameter();
            rp.setParameterName(param.parameterName().strip());
            rp.setValue(param.value().strip());
            rp.setUnit(blankToNull(param.unit()));
            rp.setReferenceMin(blankToNull(param.referenceMin()));
            rp.setReferenceMax(blankToNull(param.referenceMax()));
            rp.setAbnormal(param.abnormal());
            result.addParameter(rp);
        }
        ExamResult saved = examResultRepository.save(result);
        examRequest.setStatus(ExamRequestStatus.RESULTS_AVAILABLE);
        publish("EXAM_RESULT_RECORDED", "ExamRequest", requestId);
        return toResultResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExamResultResponse getResult(Long requestId) {
        ExamResult result = examResultRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("Résultat introuvable pour la demande " + requestId));
        return toResultResponse(result);
    }

    private ExamRequest findRequest(Long id) {
        return examRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Demande d'examen introuvable : " + id));
    }

    private ExamTypeResponse toTypeResponse(ExamType type) {
        return new ExamTypeResponse(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.getCategory(),
                type.getParameters(),
                type.isActive(),
                type.getCreatedAt()
        );
    }

    private ExamRequestResponse toRequestResponse(ExamRequest request) {
        List<ExamTypeSummary> types = request.getLines().stream()
                .map(line -> new ExamTypeSummary(
                        line.getExamType().getId(),
                        line.getExamType().getName(),
                        line.getExamType().getCategory()))
                .toList();
        return new ExamRequestResponse(
                request.getId(),
                request.getPatientId(),
                request.getDoctorId(),
                request.getAdmissionId(),
                request.getRequestedAt(),
                request.getUrgency(),
                request.getStatus(),
                request.getComment(),
                types
        );
    }

    private ExamResultResponse toResultResponse(ExamResult result) {
        List<ResultParameterResponse> params = result.getParameters().stream()
                .map(p -> new ResultParameterResponse(
                        p.getId(),
                        p.getParameterName(),
                        p.getValue(),
                        p.getUnit(),
                        p.getReferenceMin(),
                        p.getReferenceMax(),
                        p.isAbnormal()))
                .toList();
        return new ExamResultResponse(
                result.getId(),
                result.getRequest().getId(),
                result.getPatientId(),
                result.getLabTechnicianId(),
                result.getResultedAt(),
                result.getAnnotation(),
                params
        );
    }

    private void publish(String action, String resourceType, Long resourceId) {
        auditEventPublisher.publish(
                action,
                resourceType,
                String.valueOf(resourceId),
                AuditActorResolver.currentUsername(),
                null
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
