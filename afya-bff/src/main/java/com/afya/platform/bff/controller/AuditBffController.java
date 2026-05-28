package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.AuditClient;
import com.afya.platform.bff.dto.AuditEventResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditBffController {

    private final AuditClient auditClient;

    public AuditBffController(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditEventResponse> searchEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return auditClient.searchEvents(
                action,
                resourceType,
                actorUsername,
                sourceService,
                resource,
                from,
                to,
                sortBy,
                sortDir,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
