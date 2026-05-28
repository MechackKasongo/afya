package com.afya.platform.bff.support;

import com.afya.platform.bff.dto.CatalogOccupancyStatsResponse;
import com.afya.platform.bff.dto.OccupancyStatsValue;
import com.afya.platform.bff.dto.ServiceOccupancyStats;

import java.util.Collections;
import java.util.List;

public final class OccupancyStatsMapper {

    private OccupancyStatsMapper() {
    }

    public static OccupancyStatsValue toValue(CatalogOccupancyStatsResponse stats) {
        List<ServiceOccupancyStats> byService = stats.byService() != null
                ? stats.byService()
                : Collections.emptyList();
        return new OccupancyStatsValue(
                stats.overallRatePercent(),
                stats.totalBeds(),
                stats.occupiedBeds(),
                stats.availableBeds(),
                byService);
    }
}
