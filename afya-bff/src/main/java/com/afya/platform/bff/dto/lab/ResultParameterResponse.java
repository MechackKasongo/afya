package com.afya.platform.bff.dto.lab;

public record ResultParameterResponse(
        Long id,
        String parameterName,
        String value,
        String unit,
        String referenceMin,
        String referenceMax,
        boolean abnormal
) {
}
