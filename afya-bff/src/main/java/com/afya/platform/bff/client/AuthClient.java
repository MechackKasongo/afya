package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.LoginRequest;
import com.afya.platform.bff.dto.MeResponse;
import com.afya.platform.bff.dto.RefreshRequest;
import com.afya.platform.bff.dto.TokenResponse;
import com.afya.platform.bff.support.AuthorizationSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthClient {

    private final RestClient restClient;

    public AuthClient(
            @Value("${app.services.auth-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public TokenResponse login(LoginRequest request) {
        return restClient.post()
                .uri("/api/v1/auth/login")
                .body(request)
                .retrieve()
                .body(TokenResponse.class);
    }

    public TokenResponse refresh(RefreshRequest request) {
        return restClient.post()
                .uri("/api/v1/auth/refresh")
                .body(request)
                .retrieve()
                .body(TokenResponse.class);
    }

    public MeResponse me(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/auth/me")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(MeResponse.class);
    }

    public void logout(String authorizationHeader) {
        restClient.post()
                .uri("/api/v1/auth/logout")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }
}
