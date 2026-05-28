package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ClinicalRecordClient;
import com.afya.platform.bff.dto.clinical.MedicationAdministrationRequest;
import com.afya.platform.bff.dto.clinical.MedicationAdministrationResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionBffController {

    private final ClinicalRecordClient clinicalRecordClient;

    public PrescriptionBffController(ClinicalRecordClient clinicalRecordClient) {
        this.clinicalRecordClient = clinicalRecordClient;
    }

    @PostMapping("/{prescriptionLineId}/administrations")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationAdministrationResponse administer(
            @PathVariable Long prescriptionLineId,
            @RequestBody(required = false) MedicationAdministrationRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.administer(
                prescriptionLineId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
