package com.afya.platform.auth.dto;

public record LogoutRequest(
        String refreshToken,
        Boolean revokeAllSessions
) {
}
