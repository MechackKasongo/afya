package com.afya.platform.medical.dto;

import jakarta.validation.constraints.Size;

public record MedicationAdministrationRequest(
        @Size(max = 80)
        String doseGiven,
        @Size(max = 500)
        String notes
) {
}
