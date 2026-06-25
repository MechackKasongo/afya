package com.afya.platform.report.dto;

import java.time.Instant;

public record LabStatsResponse(
        Instant from,
        Instant to,
        long examRequests,
        long pendingRequests,
        long specimenCollected,
        long resultsAvailable,
        long abnormalParameters,
        boolean degraded,
        String notice
) {
}
