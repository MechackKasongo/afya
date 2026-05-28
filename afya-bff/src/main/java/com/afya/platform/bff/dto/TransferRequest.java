package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotNull(message = "Le service de destination est obligatoire")
        Long toHospitalServiceId,
        @Size(max = 255)
        String reason
) {
}
