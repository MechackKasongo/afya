package com.afya.platform.careentry.integration;

public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber
) {
}
