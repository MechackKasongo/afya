package com.afya.platform.catalog.controller;

import com.afya.platform.catalog.dto.HospitalServiceRequest;
import com.afya.platform.catalog.dto.HospitalServiceResponse;
import com.afya.platform.catalog.dto.HospitalServiceStatusRequest;
import com.afya.platform.catalog.service.HospitalServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hospital-services")
public class HospitalServiceController {

    private final HospitalServiceCatalogService hospitalServiceCatalogService;

    public HospitalServiceController(HospitalServiceCatalogService hospitalServiceCatalogService) {
        this.hospitalServiceCatalogService = hospitalServiceCatalogService;
    }

    @GetMapping
    public Page<HospitalServiceResponse> list(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return hospitalServiceCatalogService.list(activeOnly, page, size);
    }

    @GetMapping("/{id}")
    public HospitalServiceResponse get(@PathVariable Long id) {
        return hospitalServiceCatalogService.getById(id);
    }

    @PostMapping
    public HospitalServiceResponse create(@Valid @RequestBody HospitalServiceRequest request) {
        return hospitalServiceCatalogService.create(request);
    }

    @PutMapping("/{id}")
    public HospitalServiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceRequest request
    ) {
        return hospitalServiceCatalogService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public HospitalServiceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceStatusRequest request
    ) {
        return hospitalServiceCatalogService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        hospitalServiceCatalogService.delete(id);
    }
}
