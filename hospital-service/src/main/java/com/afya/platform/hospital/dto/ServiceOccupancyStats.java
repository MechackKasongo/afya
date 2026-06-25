package com.afya.platform.hospital.dto;

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
