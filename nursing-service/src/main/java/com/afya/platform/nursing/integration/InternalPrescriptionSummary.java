package com.afya.platform.nursing.integration;

public record InternalPrescriptionSummary(
        Long id,
        Long medicalRecordId,
        Long patientId,
        Long admissionId,
        String status,
        String drugName
) {
}
