package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.StayClient;
import com.afya.platform.bff.dto.AdmissionClinicalFormRequest;
import com.afya.platform.bff.dto.AdmissionClinicalFormResponse;
import com.afya.platform.bff.dto.StayResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Compatibilité UI : formulaire clinique d'hospitalisation via l'id d'admission. */
@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionStayBffController {

    private final StayClient stayClient;

    public AdmissionStayBffController(StayClient stayClient) {
        this.stayClient = stayClient;
    }

    @GetMapping("/{admissionId}/clinical-form")
    public AdmissionClinicalFormResponse getClinicalForm(
            @PathVariable Long admissionId,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        StayResponse stay = stayClient.getByAdmission(admissionId, auth);
        return stayClient.getClinicalForm(stay.id(), auth);
    }

    @PutMapping("/{admissionId}/clinical-form")
    public AdmissionClinicalFormResponse upsertClinicalForm(
            @PathVariable Long admissionId,
            @Valid @RequestBody AdmissionClinicalFormRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        StayResponse stay = stayClient.getByAdmission(admissionId, auth);
        return stayClient.upsertClinicalForm(stay.id(), body, auth);
    }
}
