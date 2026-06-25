package com.afya.platform.bff.dto;

public record OperationalStatsResponse(
        ActivityReportResponse activity,
        LabStatsResponse lab,
        NursingStatsResponse nursing
) {
}
