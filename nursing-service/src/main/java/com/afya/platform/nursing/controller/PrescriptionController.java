package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.MedicationAdministrationRequest;
import com.afya.platform.nursing.dto.MedicationAdministrationResponse;
import com.afya.platform.nursing.service.AuthorizationHeaderSupport;
import com.afya.platform.nursing.service.NursingCareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final NursingCareService nursingCareService;

    public PrescriptionController(NursingCareService nursingCareService) {
        this.nursingCareService = nursingCareService;
    }

    @PostMapping("/{prescriptionLineId}/administrations")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationAdministrationResponse administer(
            @PathVariable Long prescriptionLineId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody(required = false) MedicationAdministrationRequest request
    ) {
        MedicationAdministrationRequest body = request != null ? request : new MedicationAdministrationRequest(null, null);
        return nursingCareService.administer(
                prescriptionLineId,
                body,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
