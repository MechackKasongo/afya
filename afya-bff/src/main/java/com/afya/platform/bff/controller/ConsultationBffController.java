package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ClinicalRecordClient;
import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationBffController {

    private final ClinicalRecordClient clinicalRecordClient;

    public ConsultationBffController(ClinicalRecordClient clinicalRecordClient) {
        this.clinicalRecordClient = clinicalRecordClient;
    }

    @GetMapping("/patient-timeline")
    public java.util.List<ConsultationEventResponse> clinicalTimeline(
            @RequestParam Long patientId,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.patientClinicalTimeline(
                patientId,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping
    public Page<ConsultationResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.listConsultations(
                patientId,
                admissionId,
                page,
                size,
                sortBy,
                sortDir,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{consultationId:\\d+}")
    public ConsultationResponse getById(@PathVariable Long consultationId, HttpServletRequest request) {
        return clinicalRecordClient.getConsultation(
                consultationId,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{consultationId:\\d+}/events")
    public java.util.List<ConsultationEventResponse> consultationEvents(
            @PathVariable Long consultationId,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.consultationEvents(
                consultationId,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationResponse create(
            @Valid @RequestBody ConsultationCreateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.createConsultation(
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{consultationId:\\d+}/observations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addObservation(
            @PathVariable Long consultationId,
            @Valid @RequestBody EventCreateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addConsultationObservation(
                consultationId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{consultationId}/diagnostics")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addDiagnostic(
            @PathVariable Long consultationId,
            @Valid @RequestBody EventCreateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addConsultationDiagnostic(
                consultationId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{consultationId:\\d+}/orders/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addExamOrder(
            @PathVariable Long consultationId,
            @Valid @RequestBody EventCreateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addConsultationExamOrder(
                consultationId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
