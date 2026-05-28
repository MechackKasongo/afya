package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.CatalogClient;
import com.afya.platform.bff.dto.CatalogOccupancyStatsResponse;
import com.afya.platform.bff.dto.MetricResponse;
import com.afya.platform.bff.dto.OccupancyStatsValue;
import com.afya.platform.bff.dto.PlatformVolumesValue;
import com.afya.platform.bff.service.PlatformVolumesService;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.OccupancyStatsMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsBffController {

    private final CatalogClient catalogClient;
    private final PlatformVolumesService platformVolumesService;

    public StatsBffController(CatalogClient catalogClient, PlatformVolumesService platformVolumesService) {
        this.catalogClient = catalogClient;
        this.platformVolumesService = platformVolumesService;
    }

    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public MetricResponse occupancy(HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        CatalogOccupancyStatsResponse stats = catalogClient.occupancyStats(auth);
        OccupancyStatsValue value = OccupancyStatsMapper.toValue(stats);
        return new MetricResponse("occupancy", value, Instant.now());
    }

    @GetMapping("/volumes")
    @PreAuthorize("hasRole('ADMIN')")
    public MetricResponse volumes(HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        PlatformVolumesValue value = platformVolumesService.aggregate(auth);
        return new MetricResponse("volumes", value, Instant.now());
    }
}
