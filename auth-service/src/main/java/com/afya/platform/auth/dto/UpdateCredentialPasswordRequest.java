package com.afya.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCredentialPasswordRequest(
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
