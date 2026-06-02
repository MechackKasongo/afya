package com.afya.platform.clinical.integration;

import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class CareEntryServiceClient {

    private final RestClient restClient;

    public CareEntryServiceClient(
            @Value("${app.services.care-entry-base-url}") String baseUrl,
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

    public AdmissionSummary getAdmission(Long admissionId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/admissions/{id}", admissionId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(AdmissionSummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Admission introuvable : " + admissionId);
        }
    }
}
