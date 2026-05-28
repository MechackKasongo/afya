package com.afya.platform.clinical.controller;

import com.afya.platform.clinical.dto.PlatformVolumesResponse;
import com.afya.platform.clinical.repository.ClinicalDocumentRepository;
import com.afya.platform.clinical.repository.ConsultationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class ClinicalStatsController {

    private final ClinicalDocumentRepository clinicalDocumentRepository;
    private final ConsultationRepository consultationRepository;

    public ClinicalStatsController(
            ClinicalDocumentRepository clinicalDocumentRepository,
            ConsultationRepository consultationRepository
    ) {
        this.clinicalDocumentRepository = clinicalDocumentRepository;
        this.consultationRepository = consultationRepository;
    }

    @GetMapping("/volumes")
    public PlatformVolumesResponse volumes() {
        return new PlatformVolumesResponse(
                consultationRepository.count(),
                clinicalDocumentRepository.count());
    }
}
