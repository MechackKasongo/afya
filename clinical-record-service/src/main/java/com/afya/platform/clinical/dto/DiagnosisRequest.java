package com.afya.platform.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisRequest(
        @Size(max = 40)
        String code,
        @NotBlank(message = "Le libellé est obligatoire")
        @Size(max = 255)
        String label
) {
}
