package com.afya.platform.bff.service;

import com.afya.platform.bff.client.AuditClient;
import com.afya.platform.bff.client.CatalogClient;
import com.afya.platform.bff.dto.ActivityReportResponse;
import com.afya.platform.bff.dto.CatalogOccupancyStatsResponse;
import com.afya.platform.bff.dto.OccupancyStatsValue;
import com.afya.platform.bff.support.OccupancyStatsMapper;
import com.afya.platform.bff.dto.PlatformReportOverviewResponse;
import com.afya.platform.bff.dto.PlatformVolumesValue;
import com.afya.platform.bff.support.ActivityReportFallback;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlatformReportService {

    private final CatalogClient catalogClient;
    private final PlatformVolumesService platformVolumesService;
    private final AuditClient auditClient;

    public PlatformReportService(
            CatalogClient catalogClient,
            PlatformVolumesService platformVolumesService,
            AuditClient auditClient
    ) {
        this.catalogClient = catalogClient;
        this.platformVolumesService = platformVolumesService;
        this.auditClient = auditClient;
    }

    public PlatformReportOverviewResponse overview(String authorizationHeader, Instant from, Instant to) {
        List<String> warnings = new ArrayList<>();
        OccupancyStatsValue occupancy = loadOccupancy(authorizationHeader, warnings);
        PlatformVolumesValue volumes = loadVolumes(authorizationHeader, warnings);
        ActivityReportResponse activity = loadActivity(authorizationHeader, from, to, warnings);
        return new PlatformReportOverviewResponse(
                Instant.now(),
                occupancy,
                occupancy != null,
                volumes,
                volumes != null,
                activity,
                List.copyOf(warnings));
    }

    private OccupancyStatsValue loadOccupancy(String authorizationHeader, List<String> warnings) {
        try {
            CatalogOccupancyStatsResponse stats = catalogClient.occupancyStats(authorizationHeader);
            return OccupancyStatsMapper.toValue(stats);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            warnings.add("Occupation des lits indisponible (catalog-service).");
            return null;
        }
    }

    private PlatformVolumesValue loadVolumes(String authorizationHeader, List<String> warnings) {
        try {
            return platformVolumesService.aggregate(authorizationHeader);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            warnings.add("Volumes plateforme partiellement indisponibles.");
            return null;
        }
    }

    private ActivityReportResponse loadActivity(
            String authorizationHeader,
            Instant from,
            Instant to,
            List<String> warnings
    ) {
        try {
            return auditClient.activityReport(from, to, authorizationHeader);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            warnings.add("Rapport d'audit indisponible.");
            return ActivityReportFallback.empty(from, to);
        }
    }
}
