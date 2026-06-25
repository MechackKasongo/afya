package com.afya.platform.report.service;

import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.report.dto.OperationalStatsResponse;
import com.afya.platform.report.integration.LabStatsClient;
import com.afya.platform.report.integration.NursingStatsClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OperationalStatsService {

    private final ActivityReportService activityReportService;
    private final LabStatsClient labStatsClient;
    private final NursingStatsClient nursingStatsClient;

    public OperationalStatsService(
            ActivityReportService activityReportService,
            LabStatsClient labStatsClient,
            NursingStatsClient nursingStatsClient
    ) {
        this.activityReportService = activityReportService;
        this.labStatsClient = labStatsClient;
        this.nursingStatsClient = nursingStatsClient;
    }

    public OperationalStatsResponse operationalStats(Instant from, Instant to) {
        ActivityReportResponse activity = activityReportService.activityReport(from, to);
        return new OperationalStatsResponse(
                activity,
                labStatsClient.stats(from, to),
                nursingStatsClient.stats(from, to)
        );
    }
}
