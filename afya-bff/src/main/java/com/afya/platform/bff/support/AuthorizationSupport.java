package com.afya.platform.bff.support;

import com.afya.platform.shared.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;

public final class AuthorizationSupport {

    private AuthorizationSupport() {
    }

    public static String requireBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Jeton d'authentification manquant");
        }
        return authorizationHeader;
    }

    public static HttpHeaders bearerHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, requireBearer(authorizationHeader));
        return headers;
    }
}
