package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.AdmissionCreateRequest;
import com.afya.platform.bff.dto.CareEntryVolumesResponse;
import com.afya.platform.bff.dto.AdmissionResponse;
import com.afya.platform.bff.dto.DischargeRequest;
import com.afya.platform.bff.dto.EmergencyCreateRequest;
import com.afya.platform.bff.dto.EmergencyOrientationRequest;
import com.afya.platform.bff.dto.EmergencyResponse;
import com.afya.platform.bff.dto.EmergencyTimelineEventResponse;
import com.afya.platform.bff.dto.EmergencyTriageRequest;
import com.afya.platform.bff.dto.TransferRequest;
import com.afya.platform.bff.dto.VitalSignCreateRequest;
import com.afya.platform.bff.dto.VitalSignResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CareEntryClient {

    private final RestClient restClient;

    public CareEntryClient(
            @Value("${app.services.care-entry-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public AdmissionResponse create(AdmissionCreateRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/admissions")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public AdmissionResponse getById(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/admissions/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public Page<AdmissionResponse> list(
            Long patientId,
            String status,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/admissions");
                    if (patientId != null) {
                        builder.queryParam("patientId", patientId);
                    }
                    if (status != null && !status.isBlank()) {
                        builder.queryParam("status", status);
                    }
                    return builder
                            .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                            .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                            .build();
                },
                AdmissionResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public AdmissionResponse transfer(Long id, TransferRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/admissions/{id}/transfer", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public AdmissionResponse discharge(Long id, DischargeRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/admissions/{id}/discharge", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request != null ? request : new DischargeRequest(null))
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public AdmissionResponse declareDeath(Long id, DischargeRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/admissions/{id}/declare-death", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request != null ? request : new DischargeRequest(null))
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public AdmissionResponse cancel(Long id, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/admissions/{id}/cancel", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(AdmissionResponse.class);
    }

    public List<VitalSignResponse> listVitalSigns(Long admissionId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/admissions/{id}/vital-signs", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(VitalSignResponse.class));
    }

    public VitalSignResponse createVitalSign(
            Long admissionId,
            VitalSignCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/admissions/{id}/vital-signs", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(VitalSignResponse.class);
    }

    public CareEntryVolumesResponse volumes(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stats/volumes")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(CareEntryVolumesResponse.class);
    }

    public EmergencyResponse createEmergency(EmergencyCreateRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/urgences")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(EmergencyResponse.class);
    }

    public EmergencyResponse getEmergencyById(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/urgences/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(EmergencyResponse.class);
    }

    public List<EmergencyTimelineEventResponse> listEmergencyTimeline(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/urgences/{id}/timeline", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(EmergencyTimelineEventResponse.class));
    }

    public Page<EmergencyResponse> listEmergencies(
            String status,
            String priority,
            String sortBy,
            String sortDir,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder
                        .path("/api/v1/urgences")
                        .queryParamIfPresent("status", java.util.Optional.ofNullable(status).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("priority", java.util.Optional.ofNullable(priority).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("sortBy", java.util.Optional.ofNullable(sortBy))
                        .queryParamIfPresent("sortDir", java.util.Optional.ofNullable(sortDir))
                        .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                        .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                        .build(),
                EmergencyResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public EmergencyResponse triageEmergency(Long id, EmergencyTriageRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/urgences/{id}/triage", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(EmergencyResponse.class);
    }

    public EmergencyResponse orientEmergency(Long id, EmergencyOrientationRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/urgences/{id}/orientation", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(EmergencyResponse.class);
    }

    public EmergencyResponse closeEmergency(Long id, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/urgences/{id}/close", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(EmergencyResponse.class);
    }
}
