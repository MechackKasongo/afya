package com.afya.platform.auth.dto;

import jakarta.validation.constraints.NotNull;

public record SyncCredentialStatusRequest(
        @NotNull Boolean active
) {
}
