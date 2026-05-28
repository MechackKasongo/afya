package com.afya.platform.stay.controller;

import com.afya.platform.stay.dto.PlatformVolumesResponse;
import com.afya.platform.stay.model.StayStatus;
import com.afya.platform.stay.repository.StayRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StayStatsController {

    private final StayRepository stayRepository;

    public StayStatsController(StayRepository stayRepository) {
        this.stayRepository = stayRepository;
    }

    @GetMapping("/volumes")
    public PlatformVolumesResponse volumes() {
        return new PlatformVolumesResponse(stayRepository.countByStatus(StayStatus.EN_COURS));
    }
}
