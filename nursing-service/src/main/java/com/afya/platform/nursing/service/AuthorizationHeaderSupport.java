package com.afya.platform.nursing.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public final class AuthorizationHeaderSupport {

    private AuthorizationHeaderSupport() {
    }

    public static String requireBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("En-tête Authorization Bearer requis");
        }
        return header;
    }
}
