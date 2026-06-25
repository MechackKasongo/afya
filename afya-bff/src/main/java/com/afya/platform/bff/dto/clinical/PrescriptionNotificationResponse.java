package com.afya.platform.bff.dto.clinical;

import com.afya.platform.bff.dto.PrescriptionNotificationStatus;

import java.time.Instant;

public record PrescriptionNotificationResponse(
        Long id,
        Long prescriptionLineId,
        Long patientId,
        String drugName,
        String nurseUsername,
        Long medicationAdministrationId,
        Instant sentAt,
        PrescriptionNotificationStatus status,
        Instant readAt,
        Instant executedAt
) {
}
