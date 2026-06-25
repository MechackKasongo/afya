package com.afya.platform.nursing.service;

import com.afya.platform.nursing.dto.CreatePrescriptionNotificationRequest;
import com.afya.platform.nursing.dto.PrescriptionNotificationResponse;
import com.afya.platform.nursing.model.PrescriptionNotification;
import com.afya.platform.nursing.model.PrescriptionNotificationStatus;
import com.afya.platform.nursing.repository.PrescriptionNotificationRepository;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PrescriptionNotificationService {

    private final PrescriptionNotificationRepository prescriptionNotificationRepository;

    public PrescriptionNotificationService(PrescriptionNotificationRepository prescriptionNotificationRepository) {
        this.prescriptionNotificationRepository = prescriptionNotificationRepository;
    }

    @Transactional
    public PrescriptionNotificationResponse create(CreatePrescriptionNotificationRequest request) {
        return prescriptionNotificationRepository.findByPrescriptionLineId(request.prescriptionLineId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    PrescriptionNotification notification = new PrescriptionNotification();
                    notification.setPrescriptionLineId(request.prescriptionLineId());
                    notification.setPatientId(request.patientId());
                    notification.setDrugName(request.drugName().strip());
                    return toResponse(prescriptionNotificationRepository.save(notification));
                });
    }

    public List<PrescriptionNotificationResponse> listByPatient(Long patientId) {
        return prescriptionNotificationRepository.findByPatientIdOrderBySentAtDesc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PrescriptionNotificationResponse markRead(Long patientId, Long notificationId, String nurseUsername) {
        PrescriptionNotification notification = prescriptionNotificationRepository
                .findByIdAndPatientId(notificationId, patientId)
                .orElseThrow(() -> new NotFoundException("Notification introuvable : " + notificationId));
        if (notification.getStatus() == PrescriptionNotificationStatus.ENVOYEE) {
            notification.setStatus(PrescriptionNotificationStatus.LUE);
            notification.setNurseUsername(nurseUsername);
            notification.setReadAt(Instant.now());
            notification = prescriptionNotificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public void markExecuted(Long prescriptionLineId, Long medicationAdministrationId, String nurseUsername) {
        prescriptionNotificationRepository.findByPrescriptionLineId(prescriptionLineId).ifPresent(notification -> {
            notification.setStatus(PrescriptionNotificationStatus.EXECUTEE);
            notification.setMedicationAdministrationId(medicationAdministrationId);
            notification.setNurseUsername(nurseUsername);
            notification.setExecutedAt(Instant.now());
            if (notification.getReadAt() == null) {
                notification.setReadAt(Instant.now());
            }
            prescriptionNotificationRepository.save(notification);
        });
    }

    private PrescriptionNotificationResponse toResponse(PrescriptionNotification notification) {
        return new PrescriptionNotificationResponse(
                notification.getId(),
                notification.getPrescriptionLineId(),
                notification.getPatientId(),
                notification.getDrugName(),
                notification.getNurseUsername(),
                notification.getMedicationAdministrationId(),
                notification.getSentAt(),
                notification.getStatus(),
                notification.getReadAt(),
                notification.getExecutedAt());
    }
}
