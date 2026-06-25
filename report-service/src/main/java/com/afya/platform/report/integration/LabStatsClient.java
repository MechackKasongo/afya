package com.afya.platform.report.integration;

import com.afya.platform.report.dto.LabStatsResponse;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;

@Component
public class LabStatsClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String serviceKey;

    public LabStatsClient(
            @Value("${app.services.lab-base-url}") String baseUrl,
            @Value("${app.internal.service-key}") String serviceKey,
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
        this.serviceKey = serviceKey;
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

    public LabStatsResponse stats(Instant from, Instant to) {
        try {
            LabStatsResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/internal/reports/lab-stats");
                        if (from != null) {
                            builder.queryParam("from", from);
                        }
                        if (to != null) {
                            builder.queryParam("to", to);
                        }
                        return builder.build();
                    })
                    .header(INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .body(LabStatsResponse.class);
            if (response == null) {
                return degraded(from, to, "Réponse lab-service vide.");
            }
            return response;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            return degraded(from, to, "Statistiques laboratoire indisponibles (lab-service).");
        }
    }

    private static LabStatsResponse degraded(Instant from, Instant to, String notice) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        return new LabStatsResponse(rangeFrom, rangeTo, 0, 0, 0, 0, 0, true, notice);
    }
}
