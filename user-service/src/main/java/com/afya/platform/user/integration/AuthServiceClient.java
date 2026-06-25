package com.afya.platform.user.integration;

import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class AuthServiceClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String serviceKey;

    public AuthServiceClient(
            @Value("${app.services.auth-base-url}") String baseUrl,
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

    public void createCredential(Long userId, String username, String password) {
        restClient.post()
                .uri("/api/v1/internal/credentials")
                .header(INTERNAL_HEADER, serviceKey)
                .body(new CreateCredentialPayload(userId, username, password))
                .retrieve()
                .toBodilessEntity();
    }

    public void updatePassword(Long userId, String password) {
        restClient.put()
                .uri("/api/v1/internal/credentials/{userId}/password", userId)
                .header(INTERNAL_HEADER, serviceKey)
                .body(new UpdateCredentialPasswordPayload(password))
                .retrieve()
                .toBodilessEntity();
    }

    public void syncActive(Long userId, boolean active) {
        restClient.patch()
                .uri("/api/v1/internal/credentials/{userId}/status", userId)
                .header(INTERNAL_HEADER, serviceKey)
                .body(new SyncCredentialStatusPayload(active))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteCredential(Long userId) {
        restClient.delete()
                .uri("/api/v1/internal/credentials/{userId}", userId)
                .header(INTERNAL_HEADER, serviceKey)
                .retrieve()
                .toBodilessEntity();
    }

    private record CreateCredentialPayload(Long userId, String username, String password) {
    }

    private record UpdateCredentialPasswordPayload(String password) {
    }

    private record SyncCredentialStatusPayload(boolean active) {
    }
}
