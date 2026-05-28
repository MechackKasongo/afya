package com.afya.platform.patient.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.patient.dto.AppointmentCreateRequest;
import com.afya.platform.patient.dto.AppointmentResponse;
import com.afya.platform.patient.model.Appointment;
import com.afya.platform.patient.model.AppointmentStatus;
import com.afya.platform.patient.model.Patient;
import com.afya.platform.patient.repository.AppointmentRepository;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRegistryService patientRegistryService;
    private final AuditEventPublisher auditEventPublisher;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientRegistryService patientRegistryService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.appointmentRepository = appointmentRepository;
        this.patientRegistryService = patientRegistryService;
        this.auditEventPublisher = auditEventPublisher;
    }

    public List<AppointmentResponse> listByPatient(Long patientId) {
        patientRegistryService.find(patientId);
        return appointmentRepository.findByPatientIdOrderByScheduledAtDesc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AppointmentResponse create(Long patientId, AppointmentCreateRequest request) {
        Patient patient = patientRegistryService.find(patientId);
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setReason(request.reason());
        appointment.setStatus(AppointmentStatus.PLANNED);
        Appointment saved = appointmentRepository.save(appointment);
        String actor = AuditActorResolver.currentUsername();
        auditEventPublisher.publish(
                "APPOINTMENT_CREATED",
                "APPOINTMENT",
                AuditMetadata.resourceId(saved.getId()),
                actor,
                AuditMetadata.patientId(patientId));
        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancel(Long patientId, Long appointmentId) {
        Appointment appointment = findForPatient(patientId, appointmentId);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        auditEventPublisher.publish(
                "APPOINTMENT_CANCELLED",
                "APPOINTMENT",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.patientId(patientId));
        return toResponse(saved);
    }

    private Appointment findForPatient(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Rendez-vous introuvable : " + appointmentId));
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new NotFoundException("Rendez-vous introuvable pour ce patient");
        }
        return appointment;
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getScheduledAt(),
                appointment.getStatus().name(),
                appointment.getReason()
        );
    }
}
