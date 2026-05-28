package com.afya.platform.bff.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignResponse(
        Long id,
        Long admissionId,
        Instant recordedAt,
        String slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        String stoolsNote
) {
}
