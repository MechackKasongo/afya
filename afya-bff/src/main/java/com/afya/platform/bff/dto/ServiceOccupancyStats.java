package com.afya.platform.bff.dto;

public record ServiceOccupancyStats(
        long hospitalServiceId,
        String serviceName,
        String departmentName,
        long totalBeds,
        long occupiedBeds,
        long availableBeds,
        double ratePercent
) {
}
