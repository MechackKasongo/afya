package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ReportClient;
import com.afya.platform.bff.dto.ActivityReportResponse;
import com.afya.platform.bff.dto.OperationalStatsResponse;
import com.afya.platform.bff.dto.PlatformReportOverviewResponse;
import com.afya.platform.bff.service.PlatformReportService;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/operational-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public OperationalStatsResponse operationalStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        return reportClient.operationalStats(
                from,
                to,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/activity/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportActivity(
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        ResponseEntity<byte[]> downstream = reportClient.exportActivity(
                from,
                to,
                format,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
        return ResponseEntity.status(downstream.getStatusCode())
                .headers(downstream.getHeaders())
                .body(downstream.getBody());
    }
}
