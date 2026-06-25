package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.HospitalClient;
import com.afya.platform.bff.dto.BedOccupationResponse;
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

    private final HospitalClient hospitalClient;

    public HospitalServiceBffController(HospitalClient hospitalClient) {
        this.hospitalClient = hospitalClient;
    }

    @GetMapping
    public Page<HospitalServiceResponse> list(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return hospitalClient.listHospitalServices(
                activeOnly,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public HospitalServiceResponse get(@PathVariable Long id, HttpServletRequest request) {
        return hospitalClient.getById(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalServiceResponse create(
            @Valid @RequestBody HospitalServiceRequest body,
            HttpServletRequest request
    ) {
        return hospitalClient.create(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public HospitalServiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceRequest body,
            HttpServletRequest request
    ) {
        return hospitalClient.update(id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/{id}/status")
    public HospitalServiceResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody HospitalServiceStatusRequest body,
            HttpServletRequest request
    ) {
        return hospitalClient.updateStatus(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        hospitalClient.delete(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/beds")
    public List<BedResponse> listBeds(@PathVariable Long id, HttpServletRequest request) {
        return hospitalClient.listBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/bed-occupations")
    public List<BedOccupationResponse> listBedOccupations(
            @PathVariable Long id,
            @RequestParam(required = false) Long bedId,
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return hospitalClient.listBedOccupations(
                id,
                bedId,
                admissionId,
                patientId,
                activeOnly,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/beds/provision")
    public int provisionBeds(@PathVariable Long id, HttpServletRequest request) {
        return hospitalClient.provisionBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/beds/realign")
    public int realignBeds(@PathVariable Long id, HttpServletRequest request) {
        return hospitalClient.realignBeds(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
