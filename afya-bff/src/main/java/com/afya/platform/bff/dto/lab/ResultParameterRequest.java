package com.afya.platform.bff.dto.lab;

import jakarta.validation.constraints.NotBlank;

public record ResultParameterRequest(
        @NotBlank String parameterName,
        @NotBlank String value,
        String unit,
        String referenceMin,
        String referenceMax,
        boolean abnormal
) {
}
