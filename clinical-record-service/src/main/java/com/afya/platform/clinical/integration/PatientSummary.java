package com.afya.platform.clinical.integration;

public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber
) {
}
