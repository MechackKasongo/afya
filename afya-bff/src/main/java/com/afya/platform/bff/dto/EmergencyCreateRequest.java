package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EmergencyCreateRequest(
        @NotNull(message = "Le patient est obligatoire")
        Long patientId,
        Instant arrivedAt,
        @Size(max = 500)
        String triageNotes,
        @Size(max = 5)
        String priority
) {
}
