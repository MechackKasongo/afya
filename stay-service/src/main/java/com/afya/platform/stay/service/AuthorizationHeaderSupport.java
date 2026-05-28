package com.afya.platform.stay.service;

import com.afya.platform.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthorizationHeaderSupport {

    private AuthorizationHeaderSupport() {
    }

    public static String requireBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("Jeton d'accès requis");
        }
        return header;
    }
}
