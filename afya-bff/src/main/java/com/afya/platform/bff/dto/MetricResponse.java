package com.afya.platform.bff.dto;

import java.time.Instant;

public record MetricResponse(
        String metric,
        Object value,
        Instant generatedAt
) {
}
