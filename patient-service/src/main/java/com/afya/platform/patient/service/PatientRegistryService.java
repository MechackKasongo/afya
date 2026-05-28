package com.afya.platform.patient.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.patient.dto.DeathDeclarationRequest;
import com.afya.platform.patient.dto.PatientContactsUpdateRequest;
import com.afya.platform.patient.dto.PatientCreateRequest;
import com.afya.platform.patient.dto.PatientResponse;
import com.afya.platform.patient.dto.PatientUpdateRequest;
import com.afya.platform.patient.model.Patient;
import com.afya.platform.patient.repository.PatientRepository;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientRegistryService {

    private final PatientRepository patientRepository;
    private final DossierNumberGenerator dossierNumberGenerator;
    private final AuditEventPublisher auditEventPublisher;

    public PatientRegistryService(
            PatientRepository patientRepository,
            DossierNumberGenerator dossierNumberGenerator,
            AuditEventPublisher auditEventPublisher
    ) {
        this.patientRepository = patientRepository;
        this.dossierNumberGenerator = dossierNumberGenerator;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        checkDuplicate(request.firstName(), request.lastName(), request.birthDate(), request.sex(), null);
        String dossier = request.dossierNumber();
        if (dossier == null || dossier.isBlank()) {
            dossier = dossierNumberGenerator.generate();
        } else {
            dossier = dossier.strip();
            if (patientRepository.existsByDossierNumber(dossier)) {
                throw new ConflictException("Le numéro de dossier existe déjà");
            }
        }
        Patient patient = mapNew(request, dossier);
        Patient saved = patientRepository.save(patient);
        String actor = AuditActorResolver.currentUsername();
        auditEventPublisher.publish(
                "PATIENT_CREATED",
                "PATIENT",
                AuditMetadata.resourceId(saved.getId()),
                actor,
                AuditMetadata.dossierNumber(saved.getDossierNumber()));
        return toResponse(saved);
    }

    public PatientResponse getById(Long id) {
        return toResponse(find(id));
    }

    public Page<PatientResponse> search(String query, Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 100 ? 20 : size;
        String property = sortBy == null || sortBy.isBlank() ? "lastName" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, property));
        String q = query == null ? "" : query.strip();
        return patientRepository.search(q, pageable).map(this::toResponse);
    }

    @Transactional
    public PatientResponse declareDeath(Long id, DeathDeclarationRequest request) {
        Patient patient = find(id);
        if (patient.getDeceasedAt() != null) {
            throw new BadRequestException("Le décès de ce patient est déjà enregistré.");
        }
        patient.setDeceasedAt(Instant.now());
        Patient saved = patientRepository.save(patient);
        auditEventPublisher.publish(
                "PATIENT_DEATH_DECLARED",
                "PATIENT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public PatientResponse update(Long id, PatientUpdateRequest request) {
        Patient patient = find(id);
        ensureNotDeceased(patient);
        checkDuplicate(request.firstName(), request.lastName(), request.birthDate(), request.sex(), id);
        patient.setFirstName(request.firstName().strip());
        patient.setLastName(request.lastName().strip());
        patient.setPostName(blankToNull(request.postName()));
        patient.setBirthDate(request.birthDate());
        patient.setSex(request.sex().strip());
        patient.setPhone(blankToNull(request.phone()));
        patient.setEmail(blankToNull(request.email()));
        patient.setAddress(blankToNull(request.address()));
        patient.setBloodGroup(blankToNull(request.bloodGroup()));
        patient.setHeightCm(request.heightCm());
        Patient saved = patientRepository.save(patient);
        auditEventPublisher.publish(
                "PATIENT_UPDATED",
                "PATIENT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public PatientResponse updateContacts(Long id, PatientContactsUpdateRequest request) {
        Patient patient = find(id);
        ensureNotDeceased(patient);
        if (request.phone() != null) {
            patient.setPhone(blankToNull(request.phone()));
        }
        if (request.email() != null) {
            patient.setEmail(blankToNull(request.email()));
        }
        if (request.address() != null) {
            patient.setAddress(blankToNull(request.address()));
        }
        Patient saved = patientRepository.save(patient);
        auditEventPublisher.publish(
                "PATIENT_CONTACTS_UPDATED",
                "PATIENT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(saved.getId()));
        return toResponse(saved);
    }

    Patient find(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient introuvable : " + id));
    }

    private void checkDuplicate(String firstName, String lastName, java.time.LocalDate birthDate, String sex, Long excludeId) {
        String fn = firstName.strip();
        String ln = lastName.strip();
        String sx = sex.strip();
        boolean duplicate = excludeId == null
                ? patientRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDateAndSex(fn, ln, birthDate, sx)
                : patientRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDateAndSexAndIdNot(
                        fn, ln, birthDate, sx, excludeId);
        if (duplicate) {
            throw new ConflictException("Un patient similaire existe déjà dans le registre");
        }
    }

    private Patient mapNew(PatientCreateRequest request, String dossier) {
        Patient patient = new Patient();
        patient.setFirstName(request.firstName().strip());
        patient.setLastName(request.lastName().strip());
        patient.setPostName(blankToNull(request.postName()));
        patient.setDossierNumber(dossier);
        patient.setBirthDate(request.birthDate());
        patient.setSex(request.sex().strip());
        patient.setPhone(blankToNull(request.phone()));
        patient.setEmail(blankToNull(request.email()));
        patient.setAddress(blankToNull(request.address()));
        patient.setBloodGroup(blankToNull(request.bloodGroup()));
        patient.setHeightCm(request.heightCm());
        return patient;
    }

    private static void ensureNotDeceased(Patient patient) {
        if (patient.getDeceasedAt() != null) {
            throw new BadRequestException("La fiche patient est clôturée (décès enregistré).");
        }
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getPostName(),
                patient.getDossierNumber(),
                patient.getBirthDate(),
                patient.getSex(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getBloodGroup(),
                patient.getHeightCm(),
                patient.getDeceasedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
