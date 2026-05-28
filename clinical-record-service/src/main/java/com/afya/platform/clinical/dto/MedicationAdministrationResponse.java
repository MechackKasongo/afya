package com.afya.platform.clinical.dto;

import java.time.Instant;

public record MedicationAdministrationResponse(
        Long id,
        Long prescriptionLineId,
        Instant administeredAt,
        String doseGiven,
        String nurseUsername,
        String notes
) {
}
