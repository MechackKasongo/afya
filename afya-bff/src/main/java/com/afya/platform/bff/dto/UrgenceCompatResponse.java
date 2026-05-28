package com.afya.platform.bff.dto;

import java.time.Instant;

public record UrgenceCompatResponse(
        Long id,
        Long patientId,
        String motif,
        String priority,
        String triageLevel,
        String orientation,
        String status,
        Instant createdAt,
        Instant closedAt
) {
}
