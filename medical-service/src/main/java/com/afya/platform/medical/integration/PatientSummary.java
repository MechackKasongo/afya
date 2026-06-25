package com.afya.platform.medical.integration;

public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber
) {
}
