package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.AdmissionClient;
import com.afya.platform.bff.client.HospitalClient;
import com.afya.platform.bff.client.MedicalClient;
import com.afya.platform.bff.client.NursingClient;
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

    private final AdmissionClient admissionClient;
    private final MedicalClient medicalClient;
    private final NursingClient nursingClient;
    private final HospitalClient hospitalClient;
    private final HospitalServiceResolver hospitalServiceResolver;
    private final AdmissionUiEnricher admissionUiEnricher;

    public AdmissionBffController(
            AdmissionClient admissionClient,
            MedicalClient medicalClient,
            NursingClient nursingClient,
            HospitalClient hospitalClient,
            HospitalServiceResolver hospitalServiceResolver,
            AdmissionUiEnricher admissionUiEnricher
    ) {
        this.admissionClient = admissionClient;
        this.medicalClient = medicalClient;
        this.nursingClient = nursingClient;
        this.hospitalClient = hospitalClient;
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
        return hospitalClient.bedSuggestion(serviceId, auth);
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
        return admissionUiEnricher.enrichPage(admissionClient.list(patientId, status, page, size, auth), auth);
    }

    @GetMapping("/{id}")
    public AdmissionCompatMapper.AdmissionUiResponse getById(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionUiEnricher.enrich(admissionClient.getById(id, auth), auth);
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
        var service = hospitalClient.getById(hospitalServiceId, auth);
        if (service.bedCapacity() > 0 && (room == null || room.isBlank() || bed == null || bed.isBlank())) {
            BedSuggestionResponse suggestion = hospitalClient.bedSuggestion(hospitalServiceId, auth);
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
        return admissionUiEnricher.enrich(admissionClient.create(payload, auth), auth);
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
        return admissionUiEnricher.enrich(admissionClient.transfer(id, payload, auth), auth);
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
        return admissionUiEnricher.enrich(admissionClient.discharge(id, new DischargeRequest(note), auth), auth);
    }

    @PutMapping("/{id}/declare-death")
    public AdmissionCompatMapper.AdmissionUiResponse declareDeath(
            @PathVariable Long id,
            @RequestBody(required = false) DischargeLegacyRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        String note = body != null ? body.note() : null;
        return admissionUiEnricher.enrich(admissionClient.declareDeath(id, new DischargeRequest(note), auth), auth);
    }

    @PostMapping("/{id}/cancel")
    public AdmissionCompatMapper.AdmissionUiResponse cancel(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionUiEnricher.enrich(admissionClient.cancel(id, auth), auth);
    }

    @GetMapping("/{id}/vital-signs")
    public java.util.List<VitalSignResponse> listVitalSigns(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return nursingClient.listVitalSigns(id, auth);
    }

    @PostMapping("/{id}/vital-signs")
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse createVitalSign(
            @PathVariable Long id,
            @Valid @RequestBody VitalSignCreateRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return nursingClient.createVitalSign(id, withDefaultRecordedAt(body), auth);
    }

    @GetMapping("/{id}/vital-sign-alerts")
    public java.util.List<VitalSignAlertResponse> listVitalSignAlerts(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return nursingClient.listVitalSignAlerts(id, auth);
    }

    @GetMapping("/{id}/prescription-lines")
    public java.util.List<AdmissionPrescriptionLineResponse> listPrescriptionLines(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return medicalClient.listAdmissionPrescriptions(id, auth);
    }

    @PostMapping("/{id}/prescription-lines")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionPrescriptionLineResponse createPrescriptionLine(
            @PathVariable Long id,
            @Valid @RequestBody AdmissionPrescriptionLineCreateRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return medicalClient.createAdmissionPrescription(id, body, auth);
    }

    @PutMapping("/{id}/prescription-lines/{lineId}")
    public AdmissionPrescriptionLineResponse updatePrescriptionLine(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @Valid @RequestBody AdmissionPrescriptionLineUpdateRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return medicalClient.updateAdmissionPrescription(id, lineId, body, auth);
    }

    @GetMapping("/{id}/prescription-lines/{lineId}/administrations")
    public java.util.List<AdmissionMedicationAdministrationResponse> listMedicationAdministrations(
            @PathVariable Long id,
            @PathVariable Long lineId,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return nursingClient.listAdmissionMedications(id, lineId, auth);
    }

    @PostMapping("/{id}/prescription-lines/{lineId}/administrations")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionMedicationAdministrationResponse createMedicationAdministration(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @Valid @RequestBody AdmissionMedicationAdministrationCreateRequest body,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return nursingClient.createAdmissionMedication(id, lineId, body, auth);
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
                body.respiratoryRate(),
                body.temperatureCelsius(),
                body.weightKg(),
                body.spo2(),
                body.diuresisMl(),
                body.stoolsNote());
    }

    @GetMapping("/{id}/discharge")
    public DischargeRecordResponse getDischarge(@PathVariable Long id, HttpServletRequest request) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionClient.getDischarge(id, auth);
    }

    @GetMapping("/{id}/notifications")
    public java.util.List<AdmissionNotificationResponse> listNotifications(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionClient.listNotifications(id, auth);
    }

    @PatchMapping("/{id}/notifications/{notificationId}/read")
    public AdmissionNotificationResponse markNotificationRead(
            @PathVariable Long id,
            @PathVariable Long notificationId,
            HttpServletRequest request
    ) {
        String auth = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        return admissionClient.markNotificationRead(id, notificationId, auth);
    }
}
