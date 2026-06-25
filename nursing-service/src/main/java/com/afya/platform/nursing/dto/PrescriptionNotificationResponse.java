package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.PrescriptionNotificationStatus;

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
