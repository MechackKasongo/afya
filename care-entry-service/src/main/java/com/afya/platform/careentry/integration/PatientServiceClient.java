package com.afya.platform.careentry.integration;

import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class PatientServiceClient {

    private final RestClient restClient;

    public PatientServiceClient(
            @Value("${app.services.patient-base-url}") String baseUrl,
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
        this.restClient = RestClients.create(
                baseUrl,
                connectTimeout,
                readTimeout,
                retryMaxAttempts,
                retryDelay,
                circuitFailureThreshold,
                circuitOpenDuration
        );
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
