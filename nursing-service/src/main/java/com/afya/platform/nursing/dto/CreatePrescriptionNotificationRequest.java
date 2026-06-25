package com.afya.platform.nursing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePrescriptionNotificationRequest(
        @NotNull Long prescriptionLineId,
        @NotNull Long patientId,
        @NotBlank String drugName
) {
}
