package com.afya.platform.bff.dto;

import java.util.List;

public record OccupancyStatsValue(
        double overallRatePercent,
        long totalBeds,
        long occupiedBeds,
        long availableBeds,
        List<ServiceOccupancyStats> byService
) {
}
