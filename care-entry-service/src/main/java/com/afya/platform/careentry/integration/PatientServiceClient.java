package com.afya.platform.careentry.integration;

import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class PatientServiceClient {

    private final RestClient restClient;

    public PatientServiceClient(@Value("${app.services.patient-base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public PatientSummary getPatient(Long patientId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/patients/{id}", patientId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(PatientSummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Patient introuvable : " + patientId);
        }
    }

    public void recordDeceased(Long patientId, String note, String authorizationHeader) {
        try {
            restClient.put()
                    .uri("/api/v1/patients/{id}/declare-death", patientId)
                    .header("Authorization", authorizationHeader)
                    .body(new PatientDeathDeclarationRequest(note))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Patient introuvable : " + patientId);
        }
    }

    private record PatientDeathDeclarationRequest(String note) {
    }
}
