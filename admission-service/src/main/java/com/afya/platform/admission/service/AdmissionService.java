package com.afya.platform.admission.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.admission.dto.AdmissionCreateRequest;
import com.afya.platform.admission.dto.AdmissionResponse;
import com.afya.platform.admission.dto.DischargeRequest;
import com.afya.platform.admission.dto.TransferRequestDto;
import com.afya.platform.admission.integration.CatalogServiceClient;
import com.afya.platform.admission.integration.HospitalServiceSummary;
import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.model.Admission;
import com.afya.platform.admission.model.AdmissionStatus;
import com.afya.platform.admission.model.DischargeType;
import com.afya.platform.admission.model.TransferRequest;
import com.afya.platform.admission.repository.AdmissionRepository;
import com.afya.platform.admission.repository.TransferRequestRepository;
import com.afya.platform.admission.stay.dto.StayOpenRequest;
import com.afya.platform.admission.stay.dto.StayRelocateRequest;
import com.afya.platform.admission.stay.dto.StayResponse;
import com.afya.platform.admission.stay.service.StayService;
import com.afya.platform.shared.security.HospitalScopeSupport;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    private static final EnumSet<AdmissionStatus> ACTIVE_STATUSES =
            EnumSet.of(AdmissionStatus.OUVERTE, AdmissionStatus.TRANSFEREE);

    private final AdmissionRepository admissionRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PatientServiceClient patientServiceClient;
    private final CatalogServiceClient catalogServiceClient;
    private final StayService stayService;
    private final AdmissionWriter admissionWriter;
    private final AuditEventPublisher auditEventPublisher;
    private final AdmissionLifecycleService admissionLifecycleService;

    public AdmissionService(
            AdmissionRepository admissionRepository,
            TransferRequestRepository transferRequestRepository,
            PatientServiceClient patientServiceClient,
            CatalogServiceClient catalogServiceClient,
            StayService stayService,
            AdmissionWriter admissionWriter,
            AuditEventPublisher auditEventPublisher,
            AdmissionLifecycleService admissionLifecycleService
    ) {
        this.admissionRepository = admissionRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.patientServiceClient = patientServiceClient;
        this.catalogServiceClient = catalogServiceClient;
        this.stayService = stayService;
        this.admissionWriter = admissionWriter;
        this.auditEventPublisher = auditEventPublisher;
        this.admissionLifecycleService = admissionLifecycleService;
    }

    @Transactional
    public AdmissionResponse admit(AdmissionCreateRequest request, String authorizationHeader) {
        PatientSummary patient = patientServiceClient.getPatient(request.patientId(), authorizationHeader);
        HospitalServiceSummary service = catalogServiceClient.getHospitalService(
                request.hospitalServiceId(), authorizationHeader);
        if (admissionRepository.existsByPatientIdAndStatusIn(request.patientId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Le patient a déjà une admission active");
        }
        String roomLabel = request.roomLabel();
        String bedLabel = request.bedLabel();
        if (service.bedCapacity() > 0) {
            var assigned = catalogServiceClient.resolveBedAssignment(
                    request.hospitalServiceId(), roomLabel, bedLabel, authorizationHeader);
            roomLabel = assigned.roomLabel();
            bedLabel = assigned.bedLabel();
        }
        Admission admission = new Admission();
        admission.setPatientId(request.patientId());
        admission.setHospitalServiceId(request.hospitalServiceId());
        admission.setAdmittedAt(request.admittedAt() != null ? request.admittedAt() : Instant.now());
        admission.setStatus(AdmissionStatus.OUVERTE);
        admission.setAdmissionReason(trimToNull(request.admissionReason()));
        Admission saved = admissionWriter.persist(admission);
        openStayForAdmission(saved, roomLabel, bedLabel, authorizationHeader);
        String actor = AuditActorResolver.currentUsername();
        auditEventPublisher.publish(
                "ADMISSION_CREATED",
                "ADMISSION",
                AuditMetadata.resourceId(saved.getId()),
                actor,
                AuditMetadata.json("patientId", saved.getPatientId(), "hospitalServiceId", saved.getHospitalServiceId()));
        admissionLifecycleService.notifyHospitalisation(saved.getId());
        return toResponse(saved, patient, service);
    }

    private void openStayForAdmission(
            Admission admission,
            String roomLabel,
            String bedLabel,
            String authorizationHeader
    ) {
        try {
            stayService.open(
                    new StayOpenRequest(
                            admission.getId(),
                            admission.getPatientId(),
                            admission.getAdmittedAt(),
                            roomLabel,
                            bedLabel),
                    authorizationHeader);
            catalogServiceClient.updateBedOccupancy(
                    admission.getHospitalServiceId(),
                    roomLabel,
                    bedLabel,
                    true,
                    admission.getPatientId(),
                    admission.getId(),
                    authorizationHeader);
        } catch (HttpClientErrorException.Conflict ex) {
            log.debug("Séjour déjà ouvert ou conflit pour l'admission {} : {}", admission.getId(), ex.getMessage());
        } catch (HttpClientErrorException ex) {
            log.error("Impossible d'ouvrir le séjour pour l'admission {}", admission.getId(), ex);
            throw new BadRequestException(
                    "Admission enregistrée mais le séjour n'a pas pu être ouvert : " + ex.getStatusCode());
        }
    }

    public AdmissionResponse getById(Long id, String authorizationHeader) {
        Admission admission = find(id);
        return enrich(admission, authorizationHeader);
    }

    public Page<AdmissionResponse> list(
            Long patientId,
            String status,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        if (patientId != null) {
            patientServiceClient.getPatient(patientId, authorizationHeader);
        }
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 20 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("admittedAt").descending());
        EnumSet<AdmissionStatus> statuses = mapUiStatus(status);
        if (statuses != null && statuses.isEmpty()) {
            return Page.empty(pageable);
        }
        Collection<Long> scopeIds = resolveScopeHospitalServiceIds();
        if (scopeIds != null && scopeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Admission> admissions = fetchAdmissions(patientId, statuses, scopeIds, pageable);
        Map<Long, PatientSummary> patientCache = new ConcurrentHashMap<>();
        Map<Long, HospitalServiceSummary> serviceCache = new ConcurrentHashMap<>();
        return admissions.map(a -> toResponse(
                a,
                patientCache.computeIfAbsent(a.getPatientId(),
                        pid -> patientServiceClient.getPatient(pid, authorizationHeader)),
                serviceCache.computeIfAbsent(a.getHospitalServiceId(),
                        sid -> catalogServiceClient.getHospitalService(sid, authorizationHeader))
        ));
    }

    private static Collection<Long> resolveScopeHospitalServiceIds() {
        if (HospitalScopeSupport.isAdmin()) {
            return null;
        }
        return HospitalScopeSupport.currentHospitalServiceIds().orElse(List.of());
    }

    private Page<Admission> fetchAdmissions(
            Long patientId,
            EnumSet<AdmissionStatus> statuses,
            Collection<Long> scopeIds,
            Pageable pageable
    ) {
        boolean hasPatient = patientId != null;
        boolean hasStatus = statuses != null;
        boolean hasScope = scopeIds != null;
        if (hasPatient && hasStatus && hasScope) {
            return admissionRepository.findByPatientIdAndStatusInAndHospitalServiceIdInOrderByAdmittedAtDesc(
                    patientId, statuses, scopeIds, pageable);
        }
        if (hasPatient && hasStatus) {
            return admissionRepository.findByPatientIdAndStatusInOrderByAdmittedAtDesc(patientId, statuses, pageable);
        }
        if (hasPatient && hasScope) {
            return admissionRepository.findByPatientIdAndHospitalServiceIdInOrderByAdmittedAtDesc(
                    patientId, scopeIds, pageable);
        }
        if (hasPatient) {
            return admissionRepository.findByPatientIdOrderByAdmittedAtDesc(patientId, pageable);
        }
        if (hasStatus && hasScope) {
            return admissionRepository.findByStatusInAndHospitalServiceIdInOrderByAdmittedAtDesc(
                    statuses, scopeIds, pageable);
        }
        if (hasStatus) {
            return admissionRepository.findByStatusInOrderByAdmittedAtDesc(statuses, pageable);
        }
        if (hasScope) {
            return admissionRepository.findByHospitalServiceIdInOrderByAdmittedAtDesc(scopeIds, pageable);
        }
        return admissionRepository.findAllByOrderByAdmittedAtDesc(pageable);
    }

    private static EnumSet<AdmissionStatus> mapUiStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toUpperCase()) {
            case "EN_COURS" -> EnumSet.of(AdmissionStatus.OUVERTE, AdmissionStatus.TRANSFEREE);
            case "TRANSFERE" -> EnumSet.of(AdmissionStatus.TRANSFEREE);
            case "SORTI" -> EnumSet.of(AdmissionStatus.SORTIE);
            case "DECEDE" -> EnumSet.of(AdmissionStatus.DECEDE);
            case "OUVERTE", "TRANSFEREE", "SORTIE", "ANNULEE" ->
                    EnumSet.of(AdmissionStatus.valueOf(status.trim().toUpperCase()));
            default -> null;
        };
    }

    @Transactional
    public AdmissionResponse transfer(Long id, TransferRequestDto request, String authorizationHeader) {
        Admission admission = find(id);
        ensureActive(admission);
        if (admission.getHospitalServiceId().equals(request.toHospitalServiceId())) {
            throw new BadRequestException("Le service de destination est identique au service actuel");
        }
        HospitalServiceSummary target = catalogServiceClient.getHospitalService(
                request.toHospitalServiceId(), authorizationHeader);
        Long fromServiceId = admission.getHospitalServiceId();
        StayResponse stay = findStayOptional(admission.getId(), authorizationHeader);
        if (stay != null) {
            releaseBed(fromServiceId, stay, admission.getId(), authorizationHeader);
        }

        String roomLabel = null;
        String bedLabel = null;
        if (target.bedCapacity() > 0) {
            CatalogServiceClient.BedAssignment assigned = catalogServiceClient.resolveBedAssignment(
                    request.toHospitalServiceId(), null, null, authorizationHeader);
            roomLabel = assigned.roomLabel();
            bedLabel = assigned.bedLabel();
        }

        TransferRequest transfer = new TransferRequest();
        transfer.setAdmission(admission);
        transfer.setFromServiceId(fromServiceId);
        transfer.setToServiceId(request.toHospitalServiceId());
        transfer.setReason(request.reason());
        transferRequestRepository.save(transfer);
        admission.setHospitalServiceId(request.toHospitalServiceId());
        admission.setStatus(AdmissionStatus.TRANSFEREE);
        Admission saved = admissionRepository.save(admission);

        if (stay != null && roomLabel != null && bedLabel != null) {
            stayService.relocateByAdmissionId(
                    saved.getId(),
                    new StayRelocateRequest(roomLabel, bedLabel),
                    authorizationHeader);
            occupyBed(
                    request.toHospitalServiceId(),
                    roomLabel,
                    bedLabel,
                    saved.getPatientId(),
                    saved.getId(),
                    authorizationHeader);
        }

        auditEventPublisher.publish(
                "ADMISSION_TRANSFERRED",
                "ADMISSION",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("fromServiceId", fromServiceId, "toServiceId", request.toHospitalServiceId()));
        admissionLifecycleService.notifyTransfer(saved.getId());
        PatientSummary patient = patientServiceClient.getPatient(saved.getPatientId(), authorizationHeader);
        return toResponse(saved, patient, target);
    }

    @Transactional
    public AdmissionResponse discharge(Long id, DischargeRequest request, String authorizationHeader) {
        Admission admission = find(id);
        ensureActive(admission);
        Instant dischargedAt = Instant.now();
        admission.setStatus(AdmissionStatus.SORTIE);
        admission.setDischargedAt(dischargedAt);
        admission.setDischargeReason(request != null ? request.resolvedInstructions() : null);
        Admission saved = admissionRepository.save(admission);
        closeStayAndFreeBed(saved, authorizationHeader);
        admissionLifecycleService.recordDischarge(
                saved.getId(),
                request != null ? request.resolvedType() : DischargeType.GUERI,
                request != null ? request.resolvedInstructions() : null,
                dischargedAt);
        auditEventPublisher.publish(
                "ADMISSION_DISCHARGED",
                "ADMISSION",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        return enrich(saved, authorizationHeader);
    }

    @Transactional
    public AdmissionResponse declareDeath(Long id, DischargeRequest request, String authorizationHeader) {
        Admission admission = find(id);
        ensureActive(admission);
        Instant dischargedAt = Instant.now();
        admission.setStatus(AdmissionStatus.DECEDE);
        admission.setDischargedAt(dischargedAt);
        admission.setDischargeReason(request != null ? request.resolvedInstructions() : null);
        Admission saved = admissionRepository.save(admission);
        closeStayAndFreeBed(saved, authorizationHeader);
        admissionLifecycleService.recordDischarge(
                saved.getId(),
                DischargeType.DECEDE,
                request != null ? request.resolvedInstructions() : null,
                dischargedAt);
        auditEventPublisher.publish(
                "ADMISSION_DEATH_DECLARED",
                "ADMISSION",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        String deathNote = request != null ? request.resolvedInstructions() : null;
        patientServiceClient.recordDeceased(saved.getPatientId(), deathNote, authorizationHeader);
        return enrich(saved, authorizationHeader);
    }

    @Transactional
    public AdmissionResponse cancel(Long id, String authorizationHeader) {
        Admission admission = find(id);
        ensureActive(admission);
        closeStayAndFreeBed(admission, authorizationHeader);
        admission.setStatus(AdmissionStatus.ANNULEE);
        admission.setDischargedAt(Instant.now());
        Admission saved = admissionRepository.save(admission);
        auditEventPublisher.publish(
                "ADMISSION_CANCELLED",
                "ADMISSION",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getPatientId()));
        return enrich(saved, authorizationHeader);
    }

    private Admission find(Long id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admission introuvable : " + id));
    }

    private static void ensureActive(Admission admission) {
        if (!ACTIVE_STATUSES.contains(admission.getStatus())) {
            throw new BadRequestException("L'admission n'est plus modifiable (statut : " + admission.getStatus() + ")");
        }
    }

    private void closeStayAndFreeBed(Admission admission, String authorizationHeader) {
        StayResponse stay = findStayOptional(admission.getId(), authorizationHeader);
        if (stay != null) {
            releaseBed(admission.getHospitalServiceId(), stay, admission.getId(), authorizationHeader);
            stayService.closeByAdmissionId(admission.getId(), authorizationHeader);
        }
    }

    private StayResponse findStayOptional(Long admissionId, String authorizationHeader) {
        try {
            return stayService.getByAdmissionId(admissionId, authorizationHeader);
        } catch (NotFoundException ex) {
            log.debug("Pas de séjour pour l'admission {}", admissionId);
            return null;
        }
    }

    private void releaseBed(
            Long hospitalServiceId,
            StayResponse stay,
            Long admissionId,
            String authorizationHeader
    ) {
        if (stay.roomLabel() == null || stay.roomLabel().isBlank()
                || stay.bedLabel() == null || stay.bedLabel().isBlank()) {
            return;
        }
        catalogServiceClient.updateBedOccupancy(
                hospitalServiceId,
                stay.roomLabel(),
                stay.bedLabel(),
                false,
                stay.patientId(),
                admissionId,
                authorizationHeader);
    }

    private void occupyBed(
            Long hospitalServiceId,
            String roomLabel,
            String bedLabel,
            Long patientId,
            Long admissionId,
            String authorizationHeader
    ) {
        if (roomLabel == null || roomLabel.isBlank() || bedLabel == null || bedLabel.isBlank()) {
            return;
        }
        catalogServiceClient.updateBedOccupancy(
                hospitalServiceId,
                roomLabel,
                bedLabel,
                true,
                patientId,
                admissionId,
                authorizationHeader);
    }

    private AdmissionResponse enrich(Admission admission, String authorizationHeader) {
        PatientSummary patient = patientServiceClient.getPatient(admission.getPatientId(), authorizationHeader);
        HospitalServiceSummary service = catalogServiceClient.getHospitalService(
                admission.getHospitalServiceId(), authorizationHeader);
        return toResponse(admission, patient, service);
    }

    private static AdmissionResponse toResponse(
            Admission admission,
            PatientSummary patient,
            HospitalServiceSummary service
    ) {
        String patientName = patient.firstName() + " " + patient.lastName();
        return new AdmissionResponse(
                admission.getId(),
                admission.getPatientId(),
                patientName,
                patient.dossierNumber(),
                admission.getHospitalServiceId(),
                service.name(),
                admission.getAdmittedAt(),
                admission.getDischargedAt(),
                admission.getStatus().name(),
                admission.getAdmissionReason(),
                admission.getDischargeReason()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
