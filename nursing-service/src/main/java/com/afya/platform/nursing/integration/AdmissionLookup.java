package com.afya.platform.nursing.integration;

public record AdmissionLookup(
        Long id,
        Long patientId,
        String status
) {
}
