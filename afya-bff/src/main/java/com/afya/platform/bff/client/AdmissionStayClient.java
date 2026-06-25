package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.AdmissionClinicalFormRequest;
import com.afya.platform.bff.dto.AdmissionClinicalFormResponse;
import com.afya.platform.bff.dto.HospitalizationFormRequest;
import com.afya.platform.bff.dto.StayVolumesResponse;
import com.afya.platform.bff.dto.HospitalizationFormResponse;
import com.afya.platform.bff.dto.StayOpenRequest;
import com.afya.platform.bff.dto.StayResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AdmissionStayClient {

    private final RestClient restClient;

    public AdmissionStayClient(
            @Value("${app.services.admission-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public StayVolumesResponse volumes(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stats/volumes")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(StayVolumesResponse.class);
    }

    public StayResponse open(StayOpenRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/stays")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(StayResponse.class);
    }

    public StayResponse getByAdmission(Long admissionId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stays/by-admission/{admissionId}", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(StayResponse.class);
    }

    public HospitalizationFormResponse getForm(Long stayId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stays/{id}/hospitalization-form", stayId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(HospitalizationFormResponse.class);
    }

    public AdmissionClinicalFormResponse getClinicalForm(Long stayId, String authorizationHeader) {
        HospitalizationFormResponse raw = getForm(stayId, authorizationHeader);
        return raw == null ? null : toClinicalForm(raw);
    }

    public AdmissionClinicalFormResponse upsertClinicalForm(
            Long stayId,
            AdmissionClinicalFormRequest request,
            String authorizationHeader
    ) {
        HospitalizationFormResponse raw = restClient.put()
                .uri("/api/v1/stays/{id}/hospitalization-form", stayId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(toStayFormRequest(request))
                .retrieve()
                .body(HospitalizationFormResponse.class);
        return raw == null ? null : toClinicalForm(raw);
    }

    public HospitalizationFormResponse upsertForm(
            Long stayId,
            HospitalizationFormRequest request,
            String authorizationHeader
    ) {
        return restClient.put()
                .uri("/api/v1/stays/{id}/hospitalization-form", stayId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(HospitalizationFormResponse.class);
    }

    private static HospitalizationFormRequest toStayFormRequest(AdmissionClinicalFormRequest request) {
        return new HospitalizationFormRequest(
                request.antecedentsText(),
                request.anamnesisText(),
                request.physicalExamPulmonaryText(),
                request.physicalExamCardiacText(),
                request.physicalExamAbdominalText(),
                request.physicalExamNeurologicalText(),
                request.physicalExamMiscText(),
                request.paraclinicalText(),
                request.conclusionText()
        );
    }

    private static AdmissionClinicalFormResponse toClinicalForm(HospitalizationFormResponse raw) {
        return new AdmissionClinicalFormResponse(
                raw.id(),
                raw.admissionId(),
                raw.stayId(),
                raw.antecedentsText(),
                raw.anamnesisText(),
                raw.physicalExamPulmonaryText(),
                raw.physicalExamCardiacText(),
                raw.physicalExamAbdominalText(),
                raw.physicalExamNeurologicalText(),
                raw.physicalExamMiscText(),
                raw.paraclinicalText(),
                raw.conclusionText(),
                raw.updatedAt()
        );
    }

    public StayResponse getById(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stays/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(StayResponse.class);
    }

    public Page<StayResponse> listByPatient(
            Long patientId,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder
                        .path("/api/v1/stays")
                        .queryParam("patientId", patientId)
                        .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                        .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                        .build(),
                StayResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public StayResponse close(Long id, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/stays/{id}/close", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(StayResponse.class);
    }
}
