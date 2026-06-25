package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.VitalSignAlertResponse;
import com.afya.platform.nursing.dto.VitalSignCreateRequest;
import com.afya.platform.nursing.dto.VitalSignResponse;
import com.afya.platform.nursing.service.AuthorizationHeaderSupport;
import com.afya.platform.nursing.service.VitalSignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    public VitalSignController(VitalSignService vitalSignService) {
        this.vitalSignService = vitalSignService;
    }

    @GetMapping("/vital-signs")
    public List<VitalSignResponse> list(
            @PathVariable Long admissionId,
            HttpServletRequest httpRequest
    ) {
        return vitalSignService.listByAdmission(
                admissionId,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/vital-signs")
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse create(
            @PathVariable Long admissionId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody VitalSignCreateRequest request
    ) {
        return vitalSignService.create(
                admissionId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/vital-sign-alerts")
    public List<VitalSignAlertResponse> listAlerts(
            @PathVariable Long admissionId,
            HttpServletRequest httpRequest
    ) {
        return vitalSignService.listAlertsByAdmission(
                admissionId,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
