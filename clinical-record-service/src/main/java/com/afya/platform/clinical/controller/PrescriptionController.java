package com.afya.platform.clinical.controller;

import com.afya.platform.clinical.dto.MedicationAdministrationRequest;
import com.afya.platform.clinical.dto.MedicationAdministrationResponse;
import com.afya.platform.clinical.service.AuthorizationHeaderSupport;
import com.afya.platform.clinical.service.ClinicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final ClinicalRecordService clinicalRecordService;

    public PrescriptionController(ClinicalRecordService clinicalRecordService) {
        this.clinicalRecordService = clinicalRecordService;
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
        return clinicalRecordService.administer(
                prescriptionLineId,
                body,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest)
        );
    }
}
