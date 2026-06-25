package com.afya.platform.medical.integration;

public record CreatePrescriptionNotificationRequest(
        Long prescriptionLineId,
        Long patientId,
        String drugName
) {
}
