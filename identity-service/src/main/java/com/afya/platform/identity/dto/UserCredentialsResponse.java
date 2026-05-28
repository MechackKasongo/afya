package com.afya.platform.identity.dto;

public record UserCredentialsResponse(
        String username,
        String password,
        boolean foundInLog,
        String loggedAt
) {
}
