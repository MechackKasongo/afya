package com.afya.platform.identity.dto;

public record LogoutRequest(
        String refreshToken,
        Boolean revokeAllSessions
) {
}
