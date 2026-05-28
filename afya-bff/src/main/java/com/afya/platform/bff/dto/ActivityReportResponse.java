package com.afya.platform.bff.dto;

import java.time.Instant;
import java.util.List;

public record ActivityReportResponse(
        Instant from,
        Instant to,
        long totalEvents,
        List<ActivityCountItem> byAction,
        List<ActivityCountItem> bySourceService,
        List<ActivityCountItem> topActors,
        List<ActivityCountItem> byDay,
        boolean degraded,
        String notice
) {
}
