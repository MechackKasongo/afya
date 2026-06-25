package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.NursingCareRequest;
import com.afya.platform.nursing.dto.NursingCareResponse;
import com.afya.platform.nursing.service.AuthorizationHeaderSupport;
import com.afya.platform.nursing.service.NursingCareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
public class NursingController {

    private final NursingCareService nursingCareService;

    public NursingController(NursingCareService nursingCareService) {
        this.nursingCareService = nursingCareService;
    }

    @GetMapping("/nursing-care")
    public List<NursingCareResponse> listNursingCare(
            @PathVariable Long patientId,
            HttpServletRequest httpRequest
    ) {
        return nursingCareService.listNursingCare(
                patientId,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/nursing-care")
    @ResponseStatus(HttpStatus.CREATED)
    public NursingCareResponse addNursingCare(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody NursingCareRequest request
    ) {
        return nursingCareService.addNursingCare(
                patientId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
