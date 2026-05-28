package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.CareEntryClient;
import com.afya.platform.bff.dto.EmergencyCreateRequest;
import com.afya.platform.bff.dto.EmergencyOrientationRequest;
import com.afya.platform.bff.dto.EmergencyResponse;
import com.afya.platform.bff.dto.EmergencyTimelineEventResponse;
import com.afya.platform.bff.dto.EmergencyTriageRequest;

import java.util.List;
import com.afya.platform.bff.dto.PageUrgenceCompatResponse;
import com.afya.platform.bff.dto.UrgenceCompatResponse;
import com.afya.platform.bff.dto.UrgenceCreateLegacyRequest;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.UrgenceCompatMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urgences")
public class EmergencyBffController {

    private final CareEntryClient careEntryClient;

    public EmergencyBffController(CareEntryClient careEntryClient) {
        this.careEntryClient = careEntryClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UrgenceCompatResponse create(@Valid @RequestBody UrgenceCreateLegacyRequest body, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        String priority = body.priority() != null && !body.priority().isBlank() ? body.priority() : "P2";
        EmergencyCreateRequest payload = new EmergencyCreateRequest(body.patientId(), null, body.motif(), priority);
        return UrgenceCompatMapper.toCompat(careEntryClient.createEmergency(payload, auth));
    }

    @GetMapping("/{id}")
    public UrgenceCompatResponse getById(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return UrgenceCompatMapper.toCompat(careEntryClient.getEmergencyById(id, auth));
    }

    @GetMapping
    public PageUrgenceCompatResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        int safePage = page == null ? 0 : page;
        int safeSize = size == null || size <= 0 ? 50 : size;
        Page<EmergencyResponse> result = careEntryClient.listEmergencies(
                status, priority, sortBy, sortDir, safePage, safeSize, auth);
        return new PageUrgenceCompatResponse(
                false,
                result.getContent().stream().map(UrgenceCompatMapper::toCompat).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
    }

    @GetMapping("/{id}/timeline")
    public List<EmergencyTimelineEventResponse> timeline(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return careEntryClient.listEmergencyTimeline(id, auth);
    }

    @PostMapping("/{id}/triage")
    public UrgenceCompatResponse triage(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyTriageRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return UrgenceCompatMapper.toCompat(careEntryClient.triageEmergency(id, body, auth));
    }

    @PostMapping("/{id}/orientation")
    public UrgenceCompatResponse orient(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyOrientationRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return UrgenceCompatMapper.toCompat(careEntryClient.orientEmergency(id, body, auth));
    }

    @PostMapping("/{id}/close")
    public UrgenceCompatResponse close(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return UrgenceCompatMapper.toCompat(careEntryClient.closeEmergency(id, auth));
    }

    @PutMapping("/{id}/close")
    public UrgenceCompatResponse closePut(@PathVariable Long id, HttpServletRequest request) {
        return close(id, request);
    }
}
