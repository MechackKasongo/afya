package com.afya.platform.careentry.integration;

import com.afya.platform.shared.http.RestClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class StayServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StayServiceClient.class);

    private final RestClient restClient;

    public StayServiceClient(
            @Value("${app.services.stay-base-url:http://localhost:8085}") String baseUrl,
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

    public StayOpenResponse open(StayOpenRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/stays")
                .header("Authorization", authorizationHeader)
                .body(request)
                .retrieve()
                .body(StayOpenResponse.class);
    }

    public StaySummary getByAdmissionId(Long admissionId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/stays/by-admission/{admissionId}", admissionId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(StaySummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public void closeByAdmissionId(Long admissionId, String authorizationHeader) {
        try {
            restClient.post()
                    .uri("/api/v1/stays/close-by-admission/{admissionId}", admissionId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Aucun séjour à clôturer pour l'admission {}", admissionId);
        }
    }
}
