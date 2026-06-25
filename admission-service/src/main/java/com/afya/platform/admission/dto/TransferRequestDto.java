package com.afya.platform.admission.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferRequestDto(
        @NotNull(message = "Le service de destination est obligatoire")
        Long toHospitalServiceId,
        @Size(max = 255)
        String reason
) {
}
