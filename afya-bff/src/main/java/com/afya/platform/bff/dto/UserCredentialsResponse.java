package com.afya.platform.bff.dto;

public record UserCredentialsResponse(
        String username,
        String password,
        boolean foundInLog,
        String loggedAt
) {
}
