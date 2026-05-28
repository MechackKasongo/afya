package com.afya.platform.stay.integration;

public record PatientSummary(
        Long id,
        String firstName,
        String lastName,
        String dossierNumber
) {
}
