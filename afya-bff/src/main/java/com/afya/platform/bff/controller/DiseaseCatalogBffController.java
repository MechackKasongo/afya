package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ClinicalRecordClient;
import com.afya.platform.bff.dto.DiseaseCatalogResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disease-catalog")
public class DiseaseCatalogBffController {

    private final ClinicalRecordClient clinicalRecordClient;

    public DiseaseCatalogBffController(ClinicalRecordClient clinicalRecordClient) {
        this.clinicalRecordClient = clinicalRecordClient;
    }

    @GetMapping
    public List<DiseaseCatalogResponse> listSelectable(
            @RequestParam String diseaseType,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.listSelectableDiseases(
                diseaseType,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
