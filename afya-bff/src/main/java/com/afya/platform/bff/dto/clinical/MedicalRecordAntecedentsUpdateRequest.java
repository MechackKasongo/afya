package com.afya.platform.bff.dto.clinical;

import jakarta.validation.constraints.Size;

public record MedicalRecordAntecedentsUpdateRequest(
        @Size(max = 16000) String antecedents
) {
}
