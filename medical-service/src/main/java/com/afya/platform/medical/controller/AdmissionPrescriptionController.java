package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.AdmissionPrescriptionLineCreateRequest;
import com.afya.platform.medical.dto.AdmissionPrescriptionLineResponse;
import com.afya.platform.medical.dto.AdmissionPrescriptionLineUpdateRequest;
import com.afya.platform.medical.service.AdmissionPrescriptionService;
import com.afya.platform.medical.service.AuthorizationHeaderSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/prescription-lines")
public class AdmissionPrescriptionController {

    private final AdmissionPrescriptionService admissionPrescriptionService;

    public AdmissionPrescriptionController(AdmissionPrescriptionService admissionPrescriptionService) {
        this.admissionPrescriptionService = admissionPrescriptionService;
    }

    @GetMapping
    public List<AdmissionPrescriptionLineResponse> list(
            @PathVariable Long admissionId,
            HttpServletRequest httpRequest
    ) {
        return admissionPrescriptionService.listByAdmission(
                admissionId,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionPrescriptionLineResponse create(
            @PathVariable Long admissionId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody AdmissionPrescriptionLineCreateRequest request
    ) {
        return admissionPrescriptionService.create(
                admissionId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PutMapping("/{lineId}")
    public AdmissionPrescriptionLineResponse update(
            @PathVariable Long admissionId,
            @PathVariable Long lineId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody AdmissionPrescriptionLineUpdateRequest request
    ) {
        return admissionPrescriptionService.update(
                admissionId,
                lineId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
