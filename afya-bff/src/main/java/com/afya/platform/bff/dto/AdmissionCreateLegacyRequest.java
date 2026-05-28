package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdmissionCreateLegacyRequest(
        @NotNull Long patientId,
        @NotNull @Size(max = 120) String serviceName,
        @Size(max = 40) String room,
        @Size(max = 40) String bed,
        @Size(max = 255) String reason
) {
}
