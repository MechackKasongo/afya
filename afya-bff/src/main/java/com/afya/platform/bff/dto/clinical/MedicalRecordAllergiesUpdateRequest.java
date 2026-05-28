package com.afya.platform.bff.dto.clinical;

import jakarta.validation.constraints.Size;

public record MedicalRecordAllergiesUpdateRequest(
        @Size(max = 4000) String allergies
) {
}
