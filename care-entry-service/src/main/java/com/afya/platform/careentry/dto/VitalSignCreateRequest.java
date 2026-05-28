package com.afya.platform.careentry.dto;

import com.afya.platform.careentry.model.VitalSignSlot;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignCreateRequest(
        Instant recordedAt,
        VitalSignSlot slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        Integer diuresisMl,
        @Size(max = 500)
        String stoolsNote
) {
}
