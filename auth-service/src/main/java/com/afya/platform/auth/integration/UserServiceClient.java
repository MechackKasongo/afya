package com.afya.platform.auth.integration;

import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class UserServiceClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String serviceKey;

    public UserServiceClient(
            @Value("${app.services.user-base-url}") String baseUrl,
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

    public AuthUserProfile findByUsername(String username) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/internal/auth-profile/by-username/{username}", username)
                    .header(INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .body(AuthUserProfile.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Utilisateur introuvable : " + username);
        }
    }

    public AuthUserProfile findById(Long id) {
        try {
            return restClient.get()
                    .uri("/api/v1/users/internal/auth-profile/{id}", id)
                    .header(INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .body(AuthUserProfile.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Utilisateur introuvable : " + id);
        }
    }
}
