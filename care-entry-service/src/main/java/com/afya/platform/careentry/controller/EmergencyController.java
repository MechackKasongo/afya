package com.afya.platform.careentry.controller;

import com.afya.platform.careentry.dto.EmergencyCreateRequest;
import com.afya.platform.careentry.dto.EmergencyOrientationRequest;
import com.afya.platform.careentry.dto.EmergencyResponse;
import com.afya.platform.careentry.dto.EmergencyTimelineEventResponse;
import com.afya.platform.careentry.dto.EmergencyTriageRequest;
import com.afya.platform.careentry.service.AuthorizationHeaderSupport;
import com.afya.platform.careentry.service.EmergencyVisitService;
import com.afya.platform.careentry.service.EmergencyVisitTimelineService;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urgences")
public class EmergencyController {

    private final EmergencyVisitService emergencyVisitService;
    private final EmergencyVisitTimelineService timelineService;

    public EmergencyController(
            EmergencyVisitService emergencyVisitService,
            EmergencyVisitTimelineService timelineService
    ) {
        this.emergencyVisitService = emergencyVisitService;
        this.timelineService = timelineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyResponse create(
            HttpServletRequest httpRequest,
            @Valid @RequestBody EmergencyCreateRequest request
    ) {
        return emergencyVisitService.create(request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}")
    public EmergencyResponse getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        return emergencyVisitService.getById(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping
    public Page<EmergencyResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest
    ) {
        return emergencyVisitService.list(
                status,
                priority,
                sortBy,
                sortDir,
                page,
                size,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/triage")
    public EmergencyResponse triage(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyTriageRequest request,
            HttpServletRequest httpRequest
    ) {
        return emergencyVisitService.triage(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/{id}/orientation")
    public EmergencyResponse orient(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyOrientationRequest request,
            HttpServletRequest httpRequest
    ) {
        return emergencyVisitService.orient(id, request, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/{id}/timeline")
    public List<EmergencyTimelineEventResponse> timeline(@PathVariable Long id) {
        return timelineService.listForVisit(id);
    }

    @PostMapping("/{id}/close")
    public EmergencyResponse close(@PathVariable Long id, HttpServletRequest httpRequest) {
        return emergencyVisitService.close(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }
}
