package com.afya.platform.report.integration;

import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuditActivityClient {

    public static final String INGESTION_HEADER = "X-Audit-Ingestion-Key";

    private final RestClient restClient;
    private final String ingestionKey;

    public AuditActivityClient(
            @Value("${app.services.audit-base-url}") String baseUrl,
            @Value("${app.audit.ingestion-key}") String ingestionKey,
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
        this.ingestionKey = ingestionKey;
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

    public ActivityReportResponse activityReport(Instant from, Instant to) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/audit/internal/reports/activity");
                    if (from != null) {
                        builder.queryParam("from", from);
                    }
                    if (to != null) {
                        builder.queryParam("to", to);
                    }
                    return builder.build();
                })
                .header(INGESTION_HEADER, ingestionKey)
                .retrieve()
                .body(ActivityReportResponse.class);
    }
}
