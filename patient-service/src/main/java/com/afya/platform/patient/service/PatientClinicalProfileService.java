package com.afya.platform.patient.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.patient.dto.EmergencyContactCreateRequest;
import com.afya.platform.patient.dto.EmergencyContactResponse;
import com.afya.platform.patient.dto.EmergencyContactUpdateRequest;
import com.afya.platform.patient.dto.MedicalAntecedentCreateRequest;
import com.afya.platform.patient.dto.MedicalAntecedentResponse;
import com.afya.platform.patient.dto.MedicalAntecedentUpdateRequest;
import com.afya.platform.patient.model.EmergencyContact;
import com.afya.platform.patient.model.MedicalAntecedent;
import com.afya.platform.patient.repository.EmergencyContactRepository;
import com.afya.platform.patient.repository.MedicalAntecedentRepository;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientClinicalProfileService {

    private final PatientRegistryService patientRegistryService;
    private final MedicalAntecedentRepository medicalAntecedentRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final AuditEventPublisher auditEventPublisher;

    public PatientClinicalProfileService(
            PatientRegistryService patientRegistryService,
            MedicalAntecedentRepository medicalAntecedentRepository,
            EmergencyContactRepository emergencyContactRepository,
            AuditEventPublisher auditEventPublisher
    ) {
        this.patientRegistryService = patientRegistryService;
        this.medicalAntecedentRepository = medicalAntecedentRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    public List<MedicalAntecedentResponse> listAntecedents(Long patientId) {
        ensurePatientExists(patientId);
        return medicalAntecedentRepository.findByPatientIdOrderByEventDateDescCreatedAtDesc(patientId).stream()
                .map(this::toAntecedentResponse)
                .toList();
    }

    @Transactional
    public MedicalAntecedentResponse createAntecedent(Long patientId, MedicalAntecedentCreateRequest request) {
        ensurePatientExists(patientId);
        MedicalAntecedent antecedent = new MedicalAntecedent();
        antecedent.setPatientId(patientId);
        antecedent.setType(request.type());
        antecedent.setDescription(request.description().strip());
        antecedent.setEventDate(request.eventDate());
        MedicalAntecedent saved = medicalAntecedentRepository.save(antecedent);
        publishAntecedentAudit("PATIENT_ANTECEDENT_CREATED", saved);
        return toAntecedentResponse(saved);
    }

    @Transactional
    public MedicalAntecedentResponse updateAntecedent(
            Long patientId,
            Long antecedentId,
            MedicalAntecedentUpdateRequest request
    ) {
        MedicalAntecedent antecedent = findAntecedent(patientId, antecedentId);
        antecedent.setType(request.type());
        antecedent.setDescription(request.description().strip());
        antecedent.setEventDate(request.eventDate());
        MedicalAntecedent saved = medicalAntecedentRepository.save(antecedent);
        publishAntecedentAudit("PATIENT_ANTECEDENT_UPDATED", saved);
        return toAntecedentResponse(saved);
    }

    @Transactional
    public void deleteAntecedent(Long patientId, Long antecedentId) {
        MedicalAntecedent antecedent = findAntecedent(patientId, antecedentId);
        medicalAntecedentRepository.delete(antecedent);
        auditEventPublisher.publish(
                "PATIENT_ANTECEDENT_DELETED",
                "PATIENT_ANTECEDENT",
                AuditMetadata.resourceId(antecedentId),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(patientId));
    }

    public List<EmergencyContactResponse> listEmergencyContacts(Long patientId) {
        ensurePatientExists(patientId);
        return emergencyContactRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(this::toContactResponse)
                .toList();
    }

    @Transactional
    public EmergencyContactResponse createEmergencyContact(Long patientId, EmergencyContactCreateRequest request) {
        ensurePatientExists(patientId);
        EmergencyContact contact = new EmergencyContact();
        contact.setPatientId(patientId);
        contact.setFirstName(request.firstName().strip());
        contact.setLastName(request.lastName().strip());
        contact.setRelationship(request.relationship().strip());
        contact.setPhone(request.phone().strip());
        EmergencyContact saved = emergencyContactRepository.save(contact);
        publishContactAudit("PATIENT_EMERGENCY_CONTACT_CREATED", saved);
        return toContactResponse(saved);
    }

    @Transactional
    public EmergencyContactResponse updateEmergencyContact(
            Long patientId,
            Long contactId,
            EmergencyContactUpdateRequest request
    ) {
        EmergencyContact contact = findContact(patientId, contactId);
        contact.setFirstName(request.firstName().strip());
        contact.setLastName(request.lastName().strip());
        contact.setRelationship(request.relationship().strip());
        contact.setPhone(request.phone().strip());
        EmergencyContact saved = emergencyContactRepository.save(contact);
        publishContactAudit("PATIENT_EMERGENCY_CONTACT_UPDATED", saved);
        return toContactResponse(saved);
    }

    @Transactional
    public void deleteEmergencyContact(Long patientId, Long contactId) {
        EmergencyContact contact = findContact(patientId, contactId);
        emergencyContactRepository.delete(contact);
        auditEventPublisher.publish(
                "PATIENT_EMERGENCY_CONTACT_DELETED",
                "PATIENT_EMERGENCY_CONTACT",
                AuditMetadata.resourceId(contactId),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(patientId));
    }

    private void ensurePatientExists(Long patientId) {
        patientRegistryService.find(patientId);
    }

    private MedicalAntecedent findAntecedent(Long patientId, Long antecedentId) {
        return medicalAntecedentRepository.findByIdAndPatientId(antecedentId, patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Antécédent médical introuvable : " + antecedentId));
    }

    private EmergencyContact findContact(Long patientId, Long contactId) {
        return emergencyContactRepository.findByIdAndPatientId(contactId, patientId)
                .orElseThrow(() -> new NotFoundException(
                        "Contact d'urgence introuvable : " + contactId));
    }

    private void publishAntecedentAudit(String action, MedicalAntecedent antecedent) {
        auditEventPublisher.publish(
                action,
                "PATIENT_ANTECEDENT",
                AuditMetadata.resourceId(antecedent.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(antecedent.getPatientId()));
    }

    private void publishContactAudit(String action, EmergencyContact contact) {
        auditEventPublisher.publish(
                action,
                "PATIENT_EMERGENCY_CONTACT",
                AuditMetadata.resourceId(contact.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(contact.getPatientId()));
    }

    private MedicalAntecedentResponse toAntecedentResponse(MedicalAntecedent antecedent) {
        return new MedicalAntecedentResponse(
                antecedent.getId(),
                antecedent.getPatientId(),
                antecedent.getType(),
                antecedent.getDescription(),
                antecedent.getEventDate(),
                antecedent.getCreatedAt());
    }

    private EmergencyContactResponse toContactResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getPatientId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getRelationship(),
                contact.getPhone(),
                contact.getCreatedAt());
    }
}
