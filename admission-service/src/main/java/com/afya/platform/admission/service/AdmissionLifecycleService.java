package com.afya.platform.admission.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.admission.dto.AdmissionNotificationResponse;
import com.afya.platform.admission.dto.DischargeRecordResponse;
import com.afya.platform.admission.model.AdmissionNotification;
import com.afya.platform.admission.model.AdmissionNotificationStatus;
import com.afya.platform.admission.model.AdmissionNotificationType;
import com.afya.platform.admission.model.DischargeRecord;
import com.afya.platform.admission.model.DischargeType;
import com.afya.platform.admission.repository.AdmissionNotificationRepository;
import com.afya.platform.admission.repository.DischargeRecordRepository;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdmissionLifecycleService {

    private final DischargeRecordRepository dischargeRecordRepository;
    private final AdmissionNotificationRepository admissionNotificationRepository;

    public AdmissionLifecycleService(
            DischargeRecordRepository dischargeRecordRepository,
            AdmissionNotificationRepository admissionNotificationRepository
    ) {
        this.dischargeRecordRepository = dischargeRecordRepository;
        this.admissionNotificationRepository = admissionNotificationRepository;
    }

    @Transactional
    public DischargeRecordResponse recordDischarge(
            Long admissionId,
            DischargeType dischargeType,
            String postDischargeInstructions,
            Instant dischargedAt
    ) {
        if (dischargeRecordRepository.findByAdmissionId(admissionId).isPresent()) {
            throw new ConflictException("Une sortie est déjà enregistrée pour cette admission");
        }
        DischargeRecord record = new DischargeRecord();
        record.setAdmissionId(admissionId);
        record.setDischargedAt(dischargedAt);
        record.setDischargeType(dischargeType);
        record.setPostDischargeInstructions(postDischargeInstructions);
        record.setRecordedByUsername(AuditActorResolver.currentUsername());
        DischargeRecord saved = dischargeRecordRepository.save(record);
        notify(admissionId, AdmissionNotificationType.SORTIE_AUTORISEE, null);
        return toDischargeResponse(saved);
    }

    @Transactional
    public void notifyHospitalisation(Long admissionId) {
        notify(admissionId, AdmissionNotificationType.HOSPITALISATION, null);
    }

    @Transactional
    public void notifyTransfer(Long admissionId) {
        notify(admissionId, AdmissionNotificationType.TRANSFERT, null);
    }

    public DischargeRecordResponse getDischarge(Long admissionId) {
        return dischargeRecordRepository.findByAdmissionId(admissionId)
                .map(this::toDischargeResponse)
                .orElseThrow(() -> new NotFoundException(
                        "Sortie introuvable pour l'admission : " + admissionId));
    }

    public List<AdmissionNotificationResponse> listNotifications(Long admissionId) {
        return admissionNotificationRepository.findByAdmissionIdOrderBySentAtDesc(admissionId).stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional
    public AdmissionNotificationResponse markNotificationRead(Long admissionId, Long notificationId) {
        AdmissionNotification notification = admissionNotificationRepository
                .findByIdAndAdmissionId(notificationId, admissionId)
                .orElseThrow(() -> new NotFoundException(
                        "Notification introuvable : " + notificationId));
        if (notification.getStatus() != AdmissionNotificationStatus.LUE) {
            notification.setStatus(AdmissionNotificationStatus.LUE);
            notification.setReadAt(Instant.now());
            notification = admissionNotificationRepository.save(notification);
        }
        return toNotificationResponse(notification);
    }

    private void notify(Long admissionId, AdmissionNotificationType type, Long recipientUserId) {
        AdmissionNotification notification = new AdmissionNotification();
        notification.setAdmissionId(admissionId);
        notification.setNotificationType(type);
        notification.setRecipientUserId(recipientUserId);
        admissionNotificationRepository.save(notification);
    }

    private DischargeRecordResponse toDischargeResponse(DischargeRecord record) {
        return new DischargeRecordResponse(
                record.getId(),
                record.getAdmissionId(),
                record.getDischargedAt(),
                record.getDischargeType(),
                record.getPostDischargeInstructions(),
                record.getRecordedByUsername(),
                record.getCreatedAt());
    }

    private AdmissionNotificationResponse toNotificationResponse(AdmissionNotification notification) {
        return new AdmissionNotificationResponse(
                notification.getId(),
                notification.getAdmissionId(),
                notification.getRecipientUserId(),
                notification.getNotificationType(),
                notification.getSentAt(),
                notification.getStatus(),
                notification.getReadAt());
    }
}
