package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ReportClient;
import com.afya.platform.bff.dto.ActivityReportResponse;
import com.afya.platform.bff.dto.PlatformReportOverviewResponse;
import com.afya.platform.bff.service.PlatformReportService;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reports")
public class AdminBffController {

    private final ReportClient reportClient;
    private final PlatformReportService platformReportService;

    public AdminBffController(ReportClient reportClient, PlatformReportService platformReportService) {
        this.reportClient = reportClient;
        this.platformReportService = platformReportService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformReportOverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        return platformReportService.overview(
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")),
                from,
                to);
    }

    @GetMapping("/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ActivityReportResponse activity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        return reportClient.activityReport(
                from,
                to,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
