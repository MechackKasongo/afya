package com.afya.platform.clinical.dto;

import jakarta.validation.constraints.Size;

public record MedicalRecordAllergiesUpdateRequest(
        @Size(max = 4000)
        String allergies
) {
}
