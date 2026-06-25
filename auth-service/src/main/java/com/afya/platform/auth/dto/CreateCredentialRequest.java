package com.afya.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCredentialRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
