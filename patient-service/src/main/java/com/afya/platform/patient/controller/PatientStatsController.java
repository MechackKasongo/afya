package com.afya.platform.patient.controller;

import com.afya.platform.patient.dto.PlatformVolumesResponse;
import com.afya.platform.patient.repository.PatientRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class PatientStatsController {

    private final PatientRepository patientRepository;

    public PatientStatsController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping("/volumes")
    public PlatformVolumesResponse volumes() {
        return new PlatformVolumesResponse(patientRepository.count());
    }
}
