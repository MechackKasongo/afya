package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.DeathDeclarationRequest;
import com.afya.platform.bff.dto.EmergencyContactCreateRequest;
import com.afya.platform.bff.dto.EmergencyContactResponse;
import com.afya.platform.bff.dto.EmergencyContactUpdateRequest;
import com.afya.platform.bff.dto.MedicalAntecedentCreateRequest;
import com.afya.platform.bff.dto.MedicalAntecedentResponse;
import com.afya.platform.bff.dto.MedicalAntecedentUpdateRequest;
import com.afya.platform.bff.dto.PatientCreateRequest;
import com.afya.platform.bff.dto.PatientVolumesResponse;
import com.afya.platform.bff.dto.PatientResponse;
import com.afya.platform.bff.dto.PatientContactsUpdateRequest;
import com.afya.platform.bff.dto.PatientUpdateRequest;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PatientClient {

    private final RestClient restClient;

    public PatientClient(
            @Value("${app.services.patient-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public Page<PatientResponse> search(
            String query,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder
                        .path("/api/v1/patients")
                        .queryParam("query", query == null ? "" : query)
                        .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                        .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                        .build(),
                PatientResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public PatientVolumesResponse volumes(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stats/volumes")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(PatientVolumesResponse.class);
    }

    public PatientResponse getById(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(PatientResponse.class);
    }

    public PatientResponse create(PatientCreateRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/patients")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(PatientResponse.class);
    }

    public PatientResponse update(Long id, PatientUpdateRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/patients/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(PatientResponse.class);
    }

    public PatientResponse updateContacts(
            Long id,
            PatientContactsUpdateRequest request,
            String authorizationHeader
    ) {
        return restClient.patch()
                .uri("/api/v1/patients/{id}/contacts", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(PatientResponse.class);
    }

    public PatientResponse declareDeath(Long id, DeathDeclarationRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/patients/{id}/declare-death", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request != null ? request : new DeathDeclarationRequest(null))
                .retrieve()
                .body(PatientResponse.class);
    }

    public List<MedicalAntecedentResponse> listMedicalAntecedents(Long patientId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/medical-antecedents", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(MedicalAntecedentResponse.class));
    }

    public MedicalAntecedentResponse createMedicalAntecedent(
            Long patientId,
            MedicalAntecedentCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/medical-antecedents", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(MedicalAntecedentResponse.class);
    }

    public MedicalAntecedentResponse updateMedicalAntecedent(
            Long patientId,
            Long antecedentId,
            MedicalAntecedentUpdateRequest request,
            String authorizationHeader
    ) {
        return restClient.put()
                .uri("/api/v1/patients/{patientId}/medical-antecedents/{antecedentId}", patientId, antecedentId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(MedicalAntecedentResponse.class);
    }

    public void deleteMedicalAntecedent(Long patientId, Long antecedentId, String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/patients/{patientId}/medical-antecedents/{antecedentId}", patientId, antecedentId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }

    public List<EmergencyContactResponse> listEmergencyContacts(Long patientId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/emergency-contacts", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(EmergencyContactResponse.class));
    }

    public EmergencyContactResponse createEmergencyContact(
            Long patientId,
            EmergencyContactCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/emergency-contacts", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(EmergencyContactResponse.class);
    }

    public EmergencyContactResponse updateEmergencyContact(
            Long patientId,
            Long contactId,
            EmergencyContactUpdateRequest request,
            String authorizationHeader
    ) {
        return restClient.put()
                .uri("/api/v1/patients/{patientId}/emergency-contacts/{contactId}", patientId, contactId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(EmergencyContactResponse.class);
    }

    public void deleteEmergencyContact(Long patientId, Long contactId, String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/patients/{patientId}/emergency-contacts/{contactId}", patientId, contactId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }
}
