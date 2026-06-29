package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.LabClient;
import com.afya.platform.bff.dto.lab.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab")
public class LabBffController {

    private final LabClient labClient;

    public LabBffController(LabClient labClient) {
        this.labClient = labClient;
    }

    @GetMapping("/exam-types")
    public List<ExamTypeResponse> listExamTypes(HttpServletRequest request) {
        return labClient.listExamTypes(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-types")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamTypeResponse createExamType(
            @Valid @RequestBody ExamTypeRequest body,
            HttpServletRequest request
    ) {
        return labClient.createExamType(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamRequestResponse createRequest(
            @Valid @RequestBody ExamRequestCreateRequest body,
            HttpServletRequest request
    ) {
        return labClient.createRequest(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/exam-requests")
    public Page<ExamRequestResponse> listRequests(
            @RequestParam(required = false) ExamRequestStatus status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) ExamUrgency urgency,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return labClient.listRequests(
                status,
                doctorId,
                patientId,
                urgency,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/exam-requests/{id:\\d+}")
    public ExamRequestResponse getRequest(@PathVariable Long id, HttpServletRequest request) {
        return labClient.getRequest(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-requests/{id:\\d+}/specimen")
    public ExamRequestResponse recordSpecimen(
            @PathVariable Long id,
            @Valid @RequestBody SpecimenCollectionRequest body,
            HttpServletRequest request
    ) {
        return labClient.recordSpecimen(id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-requests/{id:\\d+}/result")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResultResponse recordResult(
            @PathVariable Long id,
            @Valid @RequestBody ExamResultRequest body,
            HttpServletRequest request
    ) {
        return labClient.recordResult(id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/exam-requests/{id:\\d+}/result")
    public ExamResultResponse getResult(@PathVariable Long id, HttpServletRequest request) {
        return labClient.getResult(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-requests/{id:\\d+}/postpone")
    public ExamRequestResponse postponeRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PostponeRequest body,
            HttpServletRequest request
    ) {
        return labClient.postponeRequest(
                id,
                body != null ? body : new PostponeRequest(null),
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/exam-requests/{id:\\d+}/reactivate")
    public ExamRequestResponse reactivateRequest(@PathVariable Long id, HttpServletRequest request) {
        return labClient.reactivateRequest(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
