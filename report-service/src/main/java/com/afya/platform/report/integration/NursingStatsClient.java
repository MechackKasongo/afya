package com.afya.platform.report.integration;

import com.afya.platform.report.dto.NursingStatsResponse;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;

@Component
public class NursingStatsClient {

    private final RestClient restClient;
    private final String serviceKey;

    public NursingStatsClient(
            @Value("${app.services.nursing-base-url}") String baseUrl,
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

    public NursingStatsResponse stats(Instant from, Instant to) {
        try {
            NursingStatsResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/internal/reports/nursing-stats");
                        if (from != null) {
                            builder.queryParam("from", from);
                        }
                        if (to != null) {
                            builder.queryParam("to", to);
                        }
                        return builder.build();
                    })
                    .header(LabStatsClient.INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .body(NursingStatsResponse.class);
            if (response == null) {
                return degraded(from, to, "Réponse nursing-service vide.");
            }
            return response;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            return degraded(from, to, "Statistiques soins indisponibles (nursing-service).");
        }
    }

    private static NursingStatsResponse degraded(Instant from, Instant to, String notice) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        return new NursingStatsResponse(rangeFrom, rangeTo, 0, 0, 0, 0, true, notice);
    }
}
