package com.afya.platform.catalog.dto;

import java.util.List;

public record OccupancyStatsResponse(
        double overallRatePercent,
        long totalBeds,
        long occupiedBeds,
        long availableBeds,
        List<ServiceOccupancyStats> byService
) {
}
