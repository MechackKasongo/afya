package com.afya.platform.bff.client;

import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class IdentityClient {

    private final RestClient restClient;

    public IdentityClient(@Value("${app.services.identity-base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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

    public Page<UserResponse> listUsers(
            String query,
            String role,
            Boolean active,
            Long hospitalServiceId,
            Boolean withoutHospitalService,
            String sortBy,
            String sortDir,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder
                        .path("/api/v1/users")
                        .queryParamIfPresent("query", Optional.ofNullable(query))
                        .queryParamIfPresent("role", Optional.ofNullable(role))
                        .queryParamIfPresent("active", Optional.ofNullable(active))
                        .queryParamIfPresent("hospitalServiceId", Optional.ofNullable(hospitalServiceId))
                        .queryParamIfPresent("withoutHospitalService", Optional.ofNullable(withoutHospitalService))
                        .queryParamIfPresent("sortBy", Optional.ofNullable(sortBy))
                        .queryParamIfPresent("sortDir", Optional.ofNullable(sortDir))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("size", Optional.ofNullable(size))
                        .build(),
                UserResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public List<RoleOptionResponse> listRoles(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/roles")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(RoleOptionResponse.class));
    }

    public PasswordPreviewResponse passwordPreview(PasswordPreviewRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/users/password-preview")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(PasswordPreviewResponse.class);
    }

    public UserResponse createUser(UserCreateRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/users")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/users/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

    public UserResponse updateUserStatus(Long id, UserStatusRequest request, String authorizationHeader) {
        return restClient.patch()
                .uri("/api/v1/users/{id}/status", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(UserResponse.class);
    }

    public void deleteUser(Long id, String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/users/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }

    public UserResponse getUser(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(UserResponse.class);
    }

    public UserCredentialsResponse credentialsForUser(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/{id}/credentials", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(UserCredentialsResponse.class);
    }

    public CredentialsLogPreviewResponse credentialsPreview(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/credentials-log/preview")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(CredentialsLogPreviewResponse.class);
    }

    public byte[] credentialsFile(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/credentials-log")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(byte[].class);
    }

    public byte[] credentialsCsv(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/users/credentials-log.csv")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(byte[].class);
    }

    public void deleteCredentialsLog(String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/users/credentials-log")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }
}
