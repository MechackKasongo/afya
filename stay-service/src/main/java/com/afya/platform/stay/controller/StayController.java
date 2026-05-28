package com.afya.platform.stay.controller;

import com.afya.platform.stay.dto.HospitalizationFormRequest;
import com.afya.platform.stay.dto.HospitalizationFormResponse;
import com.afya.platform.stay.dto.StayOpenRequest;
import com.afya.platform.stay.dto.StayResponse;
import com.afya.platform.stay.service.AuthorizationHeaderSupport;
import com.afya.platform.stay.service.StayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stays")
public class StayController {

    private final StayService stayService;

    public StayController(StayService stayService) {
        this.stayService = stayService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StayResponse open(HttpServletRequest httpRequest, @Valid @RequestBody StayOpenRequest request) {
        return stayService.open(request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}")
    public StayResponse getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        return stayService.getById(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/by-admission/{admissionId}")
    public StayResponse getByAdmission(@PathVariable Long admissionId, HttpServletRequest httpRequest) {
        return stayService.getByAdmissionId(admissionId, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping
    public Page<StayResponse> listByPatient(
            @RequestParam Long patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest
    ) {
        return stayService.listByPatient(
                patientId, page, size, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/close")
    public StayResponse close(@PathVariable Long id, HttpServletRequest httpRequest) {
        return stayService.close(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/close-by-admission/{admissionId}")
    public StayResponse closeByAdmission(@PathVariable Long admissionId, HttpServletRequest httpRequest) {
        return stayService.closeByAdmissionId(admissionId, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}/hospitalization-form")
    public HospitalizationFormResponse getForm(@PathVariable Long id, HttpServletRequest httpRequest) {
        return stayService.getForm(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PutMapping("/{id}/hospitalization-form")
    public HospitalizationFormResponse upsertForm(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @Valid @RequestBody HospitalizationFormRequest request
    ) {
        return stayService.upsertForm(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
