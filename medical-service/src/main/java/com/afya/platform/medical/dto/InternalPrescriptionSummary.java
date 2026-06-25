package com.afya.platform.medical.dto;

public record InternalPrescriptionSummary(
        Long id,
        Long medicalRecordId,
        Long patientId,
        Long admissionId,
        String status,
        String drugName
) {
}
