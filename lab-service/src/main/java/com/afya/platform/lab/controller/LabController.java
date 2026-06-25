package com.afya.platform.lab.controller;

import com.afya.platform.lab.dto.ExamRequestCreateRequest;
import com.afya.platform.lab.dto.ExamRequestResponse;
import com.afya.platform.lab.dto.ExamResultRequest;
import com.afya.platform.lab.dto.ExamResultResponse;
import com.afya.platform.lab.dto.ExamTypeRequest;
import com.afya.platform.lab.dto.ExamTypeResponse;
import com.afya.platform.lab.dto.SpecimenCollectionRequest;
import com.afya.platform.lab.model.ExamRequestStatus;
import com.afya.platform.lab.service.LabService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping("/exam-types")
    public List<ExamTypeResponse> listExamTypes() {
        return labService.listActiveExamTypes();
    }

    @PostMapping("/exam-types")
    public ExamTypeResponse createExamType(@Valid @RequestBody ExamTypeRequest request) {
        return labService.createExamType(request);
    }

    @PostMapping("/exam-requests")
    public ExamRequestResponse createRequest(@Valid @RequestBody ExamRequestCreateRequest request) {
        return labService.createRequest(request);
    }

    @GetMapping("/exam-requests")
    public Page<ExamRequestResponse> listRequests(
            @RequestParam(required = false) ExamRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return labService.listRequests(status, pageable);
    }

    @GetMapping("/exam-requests/{id}")
    public ExamRequestResponse getRequest(@PathVariable Long id) {
        return labService.getRequest(id);
    }

    @PostMapping("/exam-requests/{id}/specimen")
    public ExamRequestResponse recordSpecimen(
            @PathVariable Long id,
            @Valid @RequestBody SpecimenCollectionRequest request) {
        return labService.recordSpecimen(id, request);
    }

    @PostMapping("/exam-requests/{id}/result")
    public ExamResultResponse recordResult(
            @PathVariable Long id,
            @Valid @RequestBody ExamResultRequest request) {
        return labService.recordResult(id, request);
    }

    @GetMapping("/exam-requests/{id}/result")
    public ExamResultResponse getResult(@PathVariable Long id) {
        return labService.getResult(id);
    }
}
