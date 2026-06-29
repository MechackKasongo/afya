package com.afya.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "Le jeton refresh est obligatoire")
        @Size(max = 4096, message = "Jeton refresh invalide") String refreshToken
) {
}
