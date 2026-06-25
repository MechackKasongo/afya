package com.afya.platform.medical.dto;

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
