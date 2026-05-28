package com.afya.platform.careentry.dto;

import com.afya.platform.careentry.model.VitalSignSlot;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignResponse(
        Long id,
        Long admissionId,
        Instant recordedAt,
        VitalSignSlot slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        String stoolsNote
) {
}
