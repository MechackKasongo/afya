package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.VitalSignSlot;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignCreateRequest(
        Instant recordedAt,
        VitalSignSlot slot,
        Integer systolicBp,
        Integer diastolicBp,
        Integer pulseBpm,
        Integer respiratoryRate,
        BigDecimal temperatureCelsius,
        BigDecimal weightKg,
        BigDecimal spo2,
        Integer diuresisMl,
        @Size(max = 500) String stoolsNote
) {
}
