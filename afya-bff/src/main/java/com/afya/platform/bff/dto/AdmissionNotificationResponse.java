package com.afya.platform.bff.dto;

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
