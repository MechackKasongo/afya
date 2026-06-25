package com.afya.platform.admission.dto;

import com.afya.platform.admission.model.DischargeType;

import java.time.Instant;

public record DischargeRecordResponse(
        Long id,
        Long admissionId,
        Instant dischargedAt,
        DischargeType dischargeType,
        String postDischargeInstructions,
        String recordedByUsername,
        Instant createdAt
) {
}
