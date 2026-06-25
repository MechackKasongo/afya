package com.afya.platform.bff.dto;

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
