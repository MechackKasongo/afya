package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignCreateRequest(
        Instant recordedAt,
        String slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        @Size(max = 500) String stoolsNote
) {
}
