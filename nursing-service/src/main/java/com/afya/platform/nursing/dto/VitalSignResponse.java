package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.VitalSignSlot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VitalSignResponse(
        Long id,
        Long patientId,
        Long admissionId,
        String nurseUsername,
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
        String stoolsNote,
        List<VitalSignAlertResponse> alerts
) {
}
