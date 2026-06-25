package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.*;
import com.afya.platform.medical.service.AuthorizationHeaderSupport;
import com.afya.platform.medical.service.ConsultationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/consultations")
    public Page<ConsultationResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long admissionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return consultationService.list(patientId, admissionId, page, size, sortBy, sortDir);
    }

    @GetMapping("/consultations/patient-timeline")
    public List<ConsultationEventResponse> consultationTimeline(
            @RequestParam Long patientId
    ) {
        return consultationService.patientTimeline(patientId);
    }

    @GetMapping("/patients/{patientId}/consultation-events")
    public List<ConsultationEventResponse> patientConsultationEvents(@PathVariable Long patientId) {
        return consultationService.patientTimeline(patientId);
    }

    @GetMapping("/consultations/{consultationId:\\d+}")
    public ConsultationResponse getById(@PathVariable Long consultationId) {
        return consultationService.getById(consultationId);
    }

    @GetMapping("/consultations/{consultationId:\\d+}/events")
    public List<ConsultationEventResponse> consultationEvents(@PathVariable Long consultationId) {
        return consultationService.consultationEvents(consultationId);
    }

    @PostMapping("/consultations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationResponse create(
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody ConsultationCreateRequest request
    ) {
        return consultationService.create(
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/consultations/{consultationId:\\d+}/observations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addObservation(
            @PathVariable Long consultationId,
            Authentication auth,
            @Valid @RequestBody EventCreateRequest request
    ) {
        return consultationService.addObservation(consultationId, request, auth.getName());
    }

    @PostMapping("/consultations/{consultationId:\\d+}/diagnostics")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addDiagnostic(
            @PathVariable Long consultationId,
            Authentication auth,
            @Valid @RequestBody EventCreateRequest request
    ) {
        return consultationService.addDiagnostic(consultationId, request, auth.getName());
    }

    @PostMapping("/consultations/{consultationId:\\d+}/orders/exams")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationEventResponse addExamOrder(
            @PathVariable Long consultationId,
            Authentication auth,
            @Valid @RequestBody EventCreateRequest request
    ) {
        return consultationService.addExamOrder(consultationId, request, auth.getName());
    }

    @GetMapping("/patients/{patientId}/clinical-timeline")
    public List<ConsultationEventResponse> patientTimeline(@PathVariable Long patientId) {
        return consultationService.patientTimeline(patientId);
    }
}
