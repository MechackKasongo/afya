package com.afya.platform.stay.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record StayOpenRequest(
        @NotNull(message = "L'admission est obligatoire")
        Long admissionId,
        @NotNull(message = "Le patient est obligatoire")
        Long patientId,
        Instant checkInAt,
        @Size(max = 40)
        String roomLabel,
        @Size(max = 40)
        String bedLabel
) {
}
