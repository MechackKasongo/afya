package com.afya.platform.nursing.integration;

public record InternalPrescriptionSummary(
        Long id,
        Long medicalRecordId,
        Long patientId,
        String status,
        String drugName
) {
}
