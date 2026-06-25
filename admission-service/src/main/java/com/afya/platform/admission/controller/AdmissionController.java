package com.afya.platform.admission.controller;

import com.afya.platform.admission.dto.AdmissionCreateRequest;
import com.afya.platform.admission.dto.AdmissionNotificationResponse;
import com.afya.platform.admission.dto.AdmissionResponse;
import com.afya.platform.admission.dto.DischargeRecordResponse;
import com.afya.platform.admission.dto.DischargeRequest;
import com.afya.platform.admission.dto.TransferRequestDto;
import com.afya.platform.admission.service.AdmissionLifecycleService;
import com.afya.platform.admission.service.AdmissionService;
import com.afya.platform.admission.service.AuthorizationHeaderSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionController {

    private final AdmissionService admissionService;
    private final AdmissionLifecycleService admissionLifecycleService;

    public AdmissionController(
            AdmissionService admissionService,
            AdmissionLifecycleService admissionLifecycleService
    ) {
        this.admissionService = admissionService;
        this.admissionLifecycleService = admissionLifecycleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionResponse create(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AdmissionCreateRequest request
    ) {
        return admissionService.admit(request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}")
    public AdmissionResponse getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        return admissionService.getById(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping
    public Page<AdmissionResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest
    ) {
        return admissionService.list(
                patientId,
                status,
                page,
                size,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/transfer")
    public AdmissionResponse transfer(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @Valid @RequestBody TransferRequestDto request
    ) {
        return admissionService.transfer(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/discharge")
    public AdmissionResponse discharge(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @RequestBody(required = false) DischargeRequest request
    ) {
        return admissionService.discharge(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/cancel")
    public AdmissionResponse cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        return admissionService.cancel(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PutMapping("/{id}/declare-death")
    public AdmissionResponse declareDeath(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @RequestBody(required = false) DischargeRequest request
    ) {
        return admissionService.declareDeath(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}/discharge")
    public DischargeRecordResponse getDischarge(@PathVariable Long id, HttpServletRequest httpRequest) {
        String auth = AuthorizationHeaderSupport.requireBearer(httpRequest);
        admissionService.getById(id, auth);
        return admissionLifecycleService.getDischarge(id);
    }

    @GetMapping("/{id}/notifications")
    public List<AdmissionNotificationResponse> listNotifications(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        String auth = AuthorizationHeaderSupport.requireBearer(httpRequest);
        admissionService.getById(id, auth);
        return admissionLifecycleService.listNotifications(id);
    }

    @PatchMapping("/{id}/notifications/{notificationId}/read")
    public AdmissionNotificationResponse markNotificationRead(
            @PathVariable Long id,
            @PathVariable Long notificationId,
            HttpServletRequest httpRequest
    ) {
        String auth = AuthorizationHeaderSupport.requireBearer(httpRequest);
        admissionService.getById(id, auth);
        return admissionLifecycleService.markNotificationRead(id, notificationId);
    }
}
