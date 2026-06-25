package com.afya.platform.lab.dto;

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
