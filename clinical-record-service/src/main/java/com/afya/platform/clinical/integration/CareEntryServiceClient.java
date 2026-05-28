package com.afya.platform.clinical.integration;

import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CareEntryServiceClient {

    private final RestClient restClient;

    public CareEntryServiceClient(@Value("${app.services.care-entry-base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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
