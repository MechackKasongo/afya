package com.afya.platform.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Le jeton refresh est obligatoire") String refreshToken
) {
}
