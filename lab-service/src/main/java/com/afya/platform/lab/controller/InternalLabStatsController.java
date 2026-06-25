package com.afya.platform.lab.controller;

import com.afya.platform.lab.dto.LabStatsResponse;
import com.afya.platform.lab.service.LabStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/internal/reports")
public class InternalLabStatsController {

    private final LabStatsService labStatsService;

    public InternalLabStatsController(LabStatsService labStatsService) {
        this.labStatsService = labStatsService;
    }

    @GetMapping("/lab-stats")
    public LabStatsResponse labStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return labStatsService.stats(from, to);
    }
}
