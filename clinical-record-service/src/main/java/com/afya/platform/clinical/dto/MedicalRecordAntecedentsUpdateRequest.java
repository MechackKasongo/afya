package com.afya.platform.clinical.dto;

import jakarta.validation.constraints.Size;

public record MedicalRecordAntecedentsUpdateRequest(
        @Size(max = 16000)
        String antecedents
) {
}
