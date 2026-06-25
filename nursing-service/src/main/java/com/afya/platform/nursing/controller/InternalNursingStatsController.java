package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.NursingStatsResponse;
import com.afya.platform.nursing.service.NursingStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/internal/reports")
public class InternalNursingStatsController {

    private final NursingStatsService nursingStatsService;

    public InternalNursingStatsController(NursingStatsService nursingStatsService) {
        this.nursingStatsService = nursingStatsService;
    }

    @GetMapping("/nursing-stats")
    public NursingStatsResponse nursingStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return nursingStatsService.stats(from, to);
    }
}
