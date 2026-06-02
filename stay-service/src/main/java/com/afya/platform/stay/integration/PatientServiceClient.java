package com.afya.platform.stay.integration;

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
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout
    ) {
        this.restClient = RestClients.create(baseUrl, connectTimeout, readTimeout);
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
}
