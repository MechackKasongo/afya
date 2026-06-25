package com.afya.platform.report.controller;

import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.report.dto.GeneratedReportDownload;
import com.afya.platform.report.dto.OperationalStatsResponse;
import com.afya.platform.report.model.ReportFormat;
import com.afya.platform.report.service.ActivityReportService;
import com.afya.platform.report.service.GeneratedReportService;
import com.afya.platform.report.service.OperationalStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/reports")
public class ActivityReportController {

    private final ActivityReportService activityReportService;
    private final OperationalStatsService operationalStatsService;
    private final GeneratedReportService generatedReportService;

    public ActivityReportController(
            ActivityReportService activityReportService,
            OperationalStatsService operationalStatsService,
            GeneratedReportService generatedReportService
    ) {
        this.activityReportService = activityReportService;
        this.operationalStatsService = operationalStatsService;
        this.generatedReportService = generatedReportService;
    }

    @GetMapping("/activity")
    public ActivityReportResponse activity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return activityReportService.activityReport(from, to);
    }

    @GetMapping("/operational-stats")
    public OperationalStatsResponse operationalStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return operationalStatsService.operationalStats(from, to);
    }

    @GetMapping("/activity/export")
    public ResponseEntity<byte[]> exportActivity(
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        ReportFormat reportFormat = ReportFormat.parse(format);
        GeneratedReportDownload download = generatedReportService.exportActivity(from, to, reportFormat);
        return asAttachment(download);
    }

    @GetMapping("/generated/{id}/download")
    public ResponseEntity<byte[]> downloadGenerated(@PathVariable Long id) {
        return asAttachment(generatedReportService.download(id));
    }

    private static ResponseEntity<byte[]> asAttachment(GeneratedReportDownload download) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.payload());
    }
}
