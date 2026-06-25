package com.afya.platform.admission.dto;

import com.afya.platform.admission.model.AdmissionNotificationStatus;
import com.afya.platform.admission.model.AdmissionNotificationType;

import java.time.Instant;

public record AdmissionNotificationResponse(
        Long id,
        Long admissionId,
        Long recipientUserId,
        AdmissionNotificationType notificationType,
        Instant sentAt,
        AdmissionNotificationStatus status,
        Instant readAt
) {
}
