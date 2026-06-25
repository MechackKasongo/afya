package com.afya.platform.report.dto;

public record OperationalStatsResponse(
        ActivityReportResponse activity,
        LabStatsResponse lab,
        NursingStatsResponse nursing
) {
}
