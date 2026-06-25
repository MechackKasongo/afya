package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.AdmissionStayClient;
import com.afya.platform.bff.dto.HospitalizationFormRequest;
import com.afya.platform.bff.dto.HospitalizationFormResponse;
import com.afya.platform.bff.dto.StayOpenRequest;
import com.afya.platform.bff.dto.StayResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stays")
public class StayBffController {

    private final AdmissionStayClient stayClient;

    public StayBffController(AdmissionStayClient stayClient) {
        this.stayClient = stayClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StayResponse open(@Valid @RequestBody StayOpenRequest body, HttpServletRequest request) {
        return stayClient.open(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public StayResponse getById(@PathVariable Long id, HttpServletRequest request) {
        return stayClient.getById(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/by-admission/{admissionId}")
    public StayResponse getByAdmission(@PathVariable Long admissionId, HttpServletRequest request) {
        return stayClient.getByAdmission(
                admissionId, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping
    public Page<StayResponse> listByPatient(
            @RequestParam Long patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return stayClient.listByPatient(
                patientId,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/close")
    public StayResponse close(@PathVariable Long id, HttpServletRequest request) {
        return stayClient.close(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/hospitalization-form")
    public HospitalizationFormResponse getForm(@PathVariable Long id, HttpServletRequest request) {
        return stayClient.getForm(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}/hospitalization-form")
    public HospitalizationFormResponse upsertForm(
            @PathVariable Long id,
            @Valid @RequestBody HospitalizationFormRequest body,
            HttpServletRequest request
    ) {
        return stayClient.upsertForm(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
