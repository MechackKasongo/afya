package com.afya.platform.careentry.controller;

import com.afya.platform.careentry.dto.VitalSignCreateRequest;
import com.afya.platform.careentry.dto.VitalSignResponse;
import com.afya.platform.careentry.service.VitalSignService;
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
@RequestMapping("/api/v1/admissions/{admissionId}/vital-signs")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    public VitalSignController(VitalSignService vitalSignService) {
        this.vitalSignService = vitalSignService;
    }

    @GetMapping
    public List<VitalSignResponse> list(@PathVariable Long admissionId) {
        return vitalSignService.listByAdmission(admissionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse create(
            @PathVariable Long admissionId,
            Authentication auth,
            @Valid @RequestBody VitalSignCreateRequest request
    ) {
        return vitalSignService.create(admissionId, request, auth.getName());
    }
}
