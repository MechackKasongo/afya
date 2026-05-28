package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UrgenceCreateLegacyRequest(
        @NotNull Long patientId,
        @Size(max = 500) String motif,
        @Size(max = 10) String priority
) {
}
