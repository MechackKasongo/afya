package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.CatalogClient;
import com.afya.platform.bff.dto.BedResponse;
import com.afya.platform.bff.dto.HospitalServiceRequest;
import com.afya.platform.bff.dto.HospitalServiceResponse;
import com.afya.platform.bff.dto.HospitalServiceStatusRequest;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital-services")
public class HospitalServiceBffController {

    private final CatalogClient catalogClient;

    public HospitalServiceBffController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @GetMapping
    public Page<HospitalServiceResponse> list(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return catalogClient.listHospitalServices(
                activeOnly,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public HospitalServiceResponse get(@PathVariable Long id, HttpServletRequest request) {
        return catalogClient.getById(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalServiceResponse create(
            @Valid @RequestBody HospitalServiceRequest body,
            HttpServletRequest request
    ) {
        return catalogClient.create(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public HospitalServiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceRequest body,
            HttpServletRequest request
    ) {
        return catalogClient.update(id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/{id}/status")
    public HospitalServiceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceStatusRequest body,
            HttpServletRequest request
    ) {
        return catalogClient.updateStatus(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        catalogClient.delete(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/beds")
    public List<BedResponse> listBeds(@PathVariable Long id, HttpServletRequest request) {
        return catalogClient.listBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/beds/provision")
    public int provisionBeds(@PathVariable Long id, HttpServletRequest request) {
        return catalogClient.provisionBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/beds/realign")
    public int realignBeds(@PathVariable Long id, HttpServletRequest request) {
        return catalogClient.realignBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
