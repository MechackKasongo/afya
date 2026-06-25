package com.afya.platform.report.service;

import com.afya.platform.report.dto.ActivityCountItem;
import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.report.integration.AuditActivityClient;
import com.afya.platform.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;

@Service
public class ActivityReportService {

    private final AuditActivityClient auditActivityClient;

    public ActivityReportService(AuditActivityClient auditActivityClient) {
        this.auditActivityClient = auditActivityClient;
    }

    public ActivityReportResponse activityReport(Instant from, Instant to) {
        Instant rangeFrom = defaultFrom(from);
        Instant rangeTo = defaultTo(to);
        validateRange(rangeFrom, rangeTo);
        try {
            ActivityReportResponse response = auditActivityClient.activityReport(rangeFrom, rangeTo);
            if (response == null) {
                return degraded(rangeFrom, rangeTo, "Agrégats d'audit indisponibles.");
            }
            return response;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            return degraded(rangeFrom, rangeTo, "Agrégats d'audit indisponibles (audit-service).");
        }
    }

    private static ActivityReportResponse degraded(Instant from, Instant to, String notice) {
        return new ActivityReportResponse(from, to, 0, List.of(), List.of(), List.of(), List.of(), true, notice);
    }

    private static Instant defaultFrom(Instant from) {
        return from != null ? from : Instant.EPOCH;
    }

    private static Instant defaultTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private static void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La borne 'from' doit être antérieure ou égale à 'to'");
        }
    }
}
