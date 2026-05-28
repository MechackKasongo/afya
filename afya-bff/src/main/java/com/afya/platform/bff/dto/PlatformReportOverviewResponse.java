package com.afya.platform.bff.dto;

import java.time.Instant;
import java.util.List;

public record PlatformReportOverviewResponse(
        Instant generatedAt,
        OccupancyStatsValue occupancy,
        boolean occupancyAvailable,
        PlatformVolumesValue volumes,
        boolean volumesAvailable,
        ActivityReportResponse activity,
        List<String> warnings
) {
}
