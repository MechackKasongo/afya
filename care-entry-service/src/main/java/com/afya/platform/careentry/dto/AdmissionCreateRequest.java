package com.afya.platform.careentry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AdmissionCreateRequest(
        @NotNull(message = "Le patient est obligatoire")
        Long patientId,
        @NotNull(message = "Le service hospitalier est obligatoire")
        Long hospitalServiceId,
        Instant admittedAt,
        @Size(max = 40) String roomLabel,
        @Size(max = 40) String bedLabel,
        @Size(max = 255) String admissionReason
) {
}
