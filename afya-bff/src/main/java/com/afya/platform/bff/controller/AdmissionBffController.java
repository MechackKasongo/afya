package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.CareEntryClient;
import com.afya.platform.bff.client.CatalogClient;
import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.support.AdmissionCompatMapper;
import com.afya.platform.bff.support.AdmissionUiEnricher;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.HospitalServiceResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionBffController {

    private final CareEntryClient careEntryClient;
    private final CatalogClient catalogClient;
    private final HospitalServiceResolver hospitalServiceResolver;
    private final AdmissionUiEnricher admissionUiEnricher;

    public AdmissionBffController(
            CareEntryClient careEntryClient,
            CatalogClient catalogClient,
            HospitalServiceResolver hospitalServiceResolver,
            AdmissionUiEnricher admissionUiEnricher
    ) {
        this.careEntryClient = careEntryClient;
        this.catalogClient = catalogClient;
        this.hospitalServiceResolver = hospitalServiceResolver;
        this.admissionUiEnricher = admissionUiEnricher;
    }

    @GetMapping("/suggestions/bed")
    public BedSuggestionResponse suggestBed(
            @RequestParam String serviceName,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        Long serviceId = hospitalServiceResolver.resolveIdByName(serviceName, auth);
        return catalogClient.bedSuggestion(serviceId, auth);
    }

    @GetMapping
    public Page<AdmissionCompatMapper.AdmissionUiResponse> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionUiEnricher.enrichPage(careEntryClient.list(patientId, status, page, size, auth), auth);
    }

    @GetMapping("/{id}")
    public AdmissionCompatMapper.AdmissionUiResponse getById(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionUiEnricher.enrich(careEntryClient.getById(id, auth), auth);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionCompatMapper.AdmissionUiResponse create(
            @Valid @RequestBody AdmissionCreateLegacyRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        Long hospitalServiceId = hospitalServiceResolver.resolveIdByName(body.serviceName(), auth);
        String room = body.room();
        String bed = body.bed();
        var service = catalogClient.getById(hospitalServiceId, auth);
        if (service.bedCapacity() > 0 && (room == null || room.isBlank() || bed == null || bed.isBlank())) {
            BedSuggestionResponse suggestion = catalogClient.bedSuggestion(hospitalServiceId, auth);
            if (!suggestion.available() || suggestion.room() == null || suggestion.bed() == null) {
                String msg = suggestion.message() != null && !suggestion.message().isBlank()
                        ? suggestion.message()
                        : "Aucun lit libre pour ce service";
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, msg);
            }
            room = suggestion.room();
            bed = suggestion.bed();
        }
        AdmissionCreateRequest payload = new AdmissionCreateRequest(
                body.patientId(),
                hospitalServiceId,
                null,
                room,
                bed,
                body.reason());
        return admissionUiEnricher.enrich(careEntryClient.create(payload, auth), auth);
    }

    @PostMapping("/{id}/transfer")
    public AdmissionCompatMapper.AdmissionUiResponse transferPost(
            @PathVariable Long id,
            @Valid @RequestBody TransferLegacyRequest body,
            HttpServletRequest request
    ) {
        return transfer(id, body, request);
    }

    @PutMapping("/{id}/transfer")
    public AdmissionCompatMapper.AdmissionUiResponse transfer(
            @PathVariable Long id,
            @Valid @RequestBody TransferLegacyRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        Long toHospitalServiceId = hospitalServiceResolver.resolveIdByName(body.toService(), auth);
        TransferRequest payload = new TransferRequest(toHospitalServiceId, body.note());
        return admissionUiEnricher.enrich(careEntryClient.transfer(id, payload, auth), auth);
    }

    @PostMapping("/{id}/discharge")
    public AdmissionCompatMapper.AdmissionUiResponse dischargePost(
            @PathVariable Long id,
            @RequestBody(required = false) DischargeLegacyRequest body,
            HttpServletRequest request
    ) {
        return discharge(id, body, request);
    }

    @PutMapping("/{id}/discharge")
    public AdmissionCompatMapper.AdmissionUiResponse discharge(
            @PathVariable Long id,
            @RequestBody(required = false) DischargeLegacyRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        String note = body != null ? body.note() : null;
        return admissionUiEnricher.enrich(careEntryClient.discharge(id, new DischargeRequest(note), auth), auth);
    }

    @PutMapping("/{id}/declare-death")
    public AdmissionCompatMapper.AdmissionUiResponse declareDeath(
            @PathVariable Long id,
            @RequestBody(required = false) DischargeLegacyRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        String note = body != null ? body.note() : null;
        return admissionUiEnricher.enrich(careEntryClient.declareDeath(id, new DischargeRequest(note), auth), auth);
    }

    @PostMapping("/{id}/cancel")
    public AdmissionCompatMapper.AdmissionUiResponse cancel(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionUiEnricher.enrich(careEntryClient.cancel(id, auth), auth);
    }

    @GetMapping("/{id}/vital-signs")
    public java.util.List<VitalSignResponse> listVitalSigns(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return careEntryClient.listVitalSigns(id, auth);
    }

    @PostMapping("/{id}/vital-signs")
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse createVitalSign(
            @PathVariable Long id,
            @Valid @RequestBody VitalSignCreateRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return careEntryClient.createVitalSign(id, withDefaultRecordedAt(body), auth);
    }

    private static VitalSignCreateRequest withDefaultRecordedAt(VitalSignCreateRequest body) {
        if (body.recordedAt() != null) {
            return body;
        }
        return new VitalSignCreateRequest(
                java.time.Instant.now(),
                body.slot(),
                body.systolicBp(),
                body.diastolicBp(),
                body.pulseBpm(),
                body.temperatureCelsius(),
                body.weightKg(),
                body.diuresisMl(),
                body.stoolsNote());
    }
}
