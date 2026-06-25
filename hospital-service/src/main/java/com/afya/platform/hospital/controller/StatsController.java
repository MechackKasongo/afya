package com.afya.platform.hospital.controller;

import com.afya.platform.hospital.dto.BedSuggestionResponse;
import com.afya.platform.hospital.dto.OccupancyStatsResponse;
import com.afya.platform.hospital.service.HospitalStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StatsController {

    private final HospitalStatsService catalogStatsService;

    public StatsController(HospitalStatsService catalogStatsService) {
        this.catalogStatsService = catalogStatsService;
    }

    @GetMapping("/stats/occupancy")
    public OccupancyStatsResponse occupancy() {
        return catalogStatsService.occupancy();
    }

    @GetMapping("/hospital-services/{serviceId}/bed-suggestion")
    public BedSuggestionResponse bedSuggestion(@PathVariable Long serviceId) {
        return catalogStatsService.suggestBed(serviceId);
    }
}
