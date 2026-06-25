package com.afya.platform.admission.integration;

public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber
) {
}
