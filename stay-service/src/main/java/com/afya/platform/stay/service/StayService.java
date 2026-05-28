package com.afya.platform.stay.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.stay.dto.HospitalizationFormRequest;
import com.afya.platform.stay.dto.HospitalizationFormResponse;
import com.afya.platform.stay.dto.StayOpenRequest;
import com.afya.platform.stay.dto.StayResponse;
import com.afya.platform.stay.integration.AdmissionSummary;
import com.afya.platform.stay.integration.CareEntryServiceClient;
import com.afya.platform.stay.integration.PatientServiceClient;
import com.afya.platform.stay.integration.PatientSummary;
import com.afya.platform.stay.model.HospitalizationForm;
import com.afya.platform.stay.model.Stay;
import com.afya.platform.stay.model.StayStatus;
import com.afya.platform.stay.repository.HospitalizationFormRepository;
import com.afya.platform.stay.repository.StayRepository;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;

@Service
public class StayService {

    private static final EnumSet<StayStatus> ACTIVE_STAY =
            EnumSet.of(StayStatus.PLANIFIE, StayStatus.EN_COURS, StayStatus.SUSPENDU);

    private final StayRepository stayRepository;
    private final HospitalizationFormRepository hospitalizationFormRepository;
    private final CareEntryServiceClient careEntryServiceClient;
    private final PatientServiceClient patientServiceClient;
    private final AuditEventPublisher auditEventPublisher;

    public StayService(
            StayRepository stayRepository,
            HospitalizationFormRepository hospitalizationFormRepository,
            CareEntryServiceClient careEntryServiceClient,
            PatientServiceClient patientServiceClient,
            AuditEventPublisher auditEventPublisher
    ) {
        this.stayRepository = stayRepository;
        this.hospitalizationFormRepository = hospitalizationFormRepository;
        this.careEntryServiceClient = careEntryServiceClient;
        this.patientServiceClient = patientServiceClient;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public StayResponse open(StayOpenRequest request, String authorizationHeader) {
        if (stayRepository.existsByAdmissionId(request.admissionId())) {
            throw new ConflictException("Un séjour existe déjà pour cette admission");
        }
        AdmissionSummary admission = careEntryServiceClient.getAdmission(request.admissionId(), authorizationHeader);
        careEntryServiceClient.ensureAdmissionActive(admission);
        if (!admission.patientId().equals(request.patientId())) {
            throw new BadRequestException("Le patient ne correspond pas à l'admission");
        }
        if (stayRepository.existsByPatientIdAndStatusIn(request.patientId(), ACTIVE_STAY)) {
            throw new ConflictException("Le patient a déjà un séjour actif");
        }
        PatientSummary patient = patientServiceClient.getPatient(request.patientId(), authorizationHeader);
        Stay stay = new Stay();
        stay.setPatientId(request.patientId());
        stay.setAdmissionId(request.admissionId());
        stay.setCheckInAt(request.checkInAt() != null ? request.checkInAt() : Instant.now());
        stay.setRoomLabel(request.roomLabel());
        stay.setBedLabel(request.bedLabel());
        stay.setStatus(StayStatus.EN_COURS);
        Stay saved = stayRepository.save(stay);
        HospitalizationForm form = new HospitalizationForm();
        form.setStay(saved);
        hospitalizationFormRepository.save(form);
        auditEventPublisher.publish(
                "STAY_OPENED",
                "STAY",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("patientId", saved.getPatientId(), "admissionId", saved.getAdmissionId()));
        return toResponse(saved, patient);
    }

    public StayResponse getById(Long id, String authorizationHeader) {
        Stay stay = find(id);
        PatientSummary patient = patientServiceClient.getPatient(stay.getPatientId(), authorizationHeader);
        return toResponse(stay, patient);
    }

    public StayResponse getByAdmissionId(Long admissionId, String authorizationHeader) {
        Stay stay = stayRepository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new NotFoundException("Séjour introuvable pour l'admission : " + admissionId));
        PatientSummary patient = patientServiceClient.getPatient(stay.getPatientId(), authorizationHeader);
        return toResponse(stay, patient);
    }

    public Page<StayResponse> listByPatient(Long patientId, Integer page, Integer size, String authorizationHeader) {
        patientServiceClient.getPatient(patientId, authorizationHeader);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 100 ? 20 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("checkInAt").descending());
        return stayRepository.findByPatientIdOrderByCheckInAtDesc(patientId, pageable)
                .map(s -> toResponse(s, patientServiceClient.getPatient(s.getPatientId(), authorizationHeader)));
    }

    @Transactional
    public StayResponse close(Long id, String authorizationHeader) {
        return closeStay(find(id), authorizationHeader);
    }

    @Transactional
    public StayResponse closeByAdmissionId(Long admissionId, String authorizationHeader) {
        Stay stay = stayRepository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new NotFoundException("Séjour introuvable pour l'admission : " + admissionId));
        return closeStay(stay, authorizationHeader);
    }

    @Transactional
    public HospitalizationFormResponse upsertForm(Long stayId, HospitalizationFormRequest request, String authorizationHeader) {
        Stay stay = find(stayId);
        ensureStayEditable(stay);
        patientServiceClient.getPatient(stay.getPatientId(), authorizationHeader);
        HospitalizationForm form = hospitalizationFormRepository.findById(stayId)
                .orElseGet(() -> {
                    HospitalizationForm created = new HospitalizationForm();
                    created.setStay(stay);
                    return created;
                });
        applyFormFields(form, request);
        form.setUpdatedAt(Instant.now());
        HospitalizationForm saved = hospitalizationFormRepository.save(form);
        auditEventPublisher.publish(
                "HOSPITALIZATION_FORM_UPDATED",
                "STAY",
                AuditMetadata.resourceId(stay.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(stay.getPatientId()));
        return toFormResponse(stay, saved);
    }

    public HospitalizationFormResponse getForm(Long stayId, String authorizationHeader) {
        Stay stay = find(stayId);
        patientServiceClient.getPatient(stay.getPatientId(), authorizationHeader);
        return hospitalizationFormRepository.findById(stayId)
                .map(form -> toFormResponse(stay, form))
                .orElseGet(() -> emptyFormResponse(stay));
    }

    private StayResponse closeStay(Stay stay, String authorizationHeader) {
        if (stay.getStatus() == StayStatus.CLOTURE || stay.getStatus() == StayStatus.ANNULE) {
            throw new BadRequestException("Le séjour est déjà clôturé");
        }
        stay.setStatus(StayStatus.CLOTURE);
        stay.setCheckOutAt(Instant.now());
        Stay saved = stayRepository.save(stay);
        auditEventPublisher.publish(
                "STAY_CLOSED",
                "STAY",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("patientId", saved.getPatientId(), "admissionId", saved.getAdmissionId()));
        PatientSummary patient = patientServiceClient.getPatient(saved.getPatientId(), authorizationHeader);
        return toResponse(saved, patient);
    }

    private Stay find(Long id) {
        return stayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Séjour introuvable : " + id));
    }

    private static void ensureStayEditable(Stay stay) {
        if (stay.getStatus() == StayStatus.CLOTURE || stay.getStatus() == StayStatus.ANNULE) {
            throw new BadRequestException("Le séjour n'est plus modifiable");
        }
    }

    private static StayResponse toResponse(Stay stay, PatientSummary patient) {
        return new StayResponse(
                stay.getId(),
                stay.getPatientId(),
                patient.firstName() + " " + patient.lastName(),
                patient.dossierNumber(),
                stay.getAdmissionId(),
                stay.getCheckInAt(),
                stay.getCheckOutAt(),
                stay.getRoomLabel(),
                stay.getBedLabel(),
                stay.getStatus().name()
        );
    }

    private static void applyFormFields(HospitalizationForm form, HospitalizationFormRequest request) {
        form.setAntecedentsText(blankToNull(request.antecedentsText()));
        form.setAnamnesisText(blankToNull(request.anamnesisText()));
        form.setPhysicalExamPulmonaryText(blankToNull(request.physicalExamPulmonaryText()));
        form.setPhysicalExamCardiacText(blankToNull(request.physicalExamCardiacText()));
        form.setPhysicalExamAbdominalText(blankToNull(request.physicalExamAbdominalText()));
        form.setPhysicalExamNeurologicalText(blankToNull(request.physicalExamNeurologicalText()));
        form.setPhysicalExamMiscText(blankToNull(request.physicalExamMiscText()));
        form.setParaclinicalText(blankToNull(request.paraclinicalText()));
        form.setConclusionText(blankToNull(request.conclusionText()));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static HospitalizationFormResponse emptyFormResponse(Stay stay) {
        return new HospitalizationFormResponse(
                stay.getId(),
                stay.getAdmissionId(),
                stay.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static HospitalizationFormResponse toFormResponse(Stay stay, HospitalizationForm form) {
        return new HospitalizationFormResponse(
                form.getStayId(),
                stay.getAdmissionId(),
                form.getStayId(),
                form.getAntecedentsText(),
                form.getAnamnesisText(),
                form.getPhysicalExamPulmonaryText(),
                form.getPhysicalExamCardiacText(),
                form.getPhysicalExamAbdominalText(),
                form.getPhysicalExamNeurologicalText(),
                form.getPhysicalExamMiscText(),
                form.getParaclinicalText(),
                form.getConclusionText(),
                form.getUpdatedAt()
        );
    }
}
