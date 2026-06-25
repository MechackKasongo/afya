package com.afya.platform.hospital.controller;

import com.afya.platform.hospital.dto.BedOccupationResponse;
import com.afya.platform.hospital.service.BedOccupationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital-services/{serviceId}/bed-occupations")
public class BedOccupationController {

    private final BedOccupationService bedOccupationService;

    public BedOccupationController(BedOccupationService bedOccupationService) {
        this.bedOccupationService = bedOccupationService;
    }

    @GetMapping
    public List<BedOccupationResponse> list(
            @PathVariable Long serviceId,
            @RequestParam(required = false) Long bedId,
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return bedOccupationService.list(serviceId, bedId, admissionId, patientId, activeOnly, page, size);
    }

    @GetMapping("/{occupationId}")
    public BedOccupationResponse get(@PathVariable Long serviceId, @PathVariable Long occupationId) {
        return bedOccupationService.getById(serviceId, occupationId);
    }
}
