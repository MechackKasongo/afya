package com.afya.platform.audit.controller;

import com.afya.platform.audit.dto.ActivityReportResponse;
import com.afya.platform.audit.service.AuditEventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit/internal/reports")
public class InternalActivityReportController {

    private final AuditEventService auditEventService;

    public InternalActivityReportController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/activity")
    public ActivityReportResponse activity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditEventService.activityReport(from, to);
    }
}
