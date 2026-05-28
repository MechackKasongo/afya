package com.afya.platform.patient.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentCreateRequest(
        @NotNull(message = "La date du rendez-vous est obligatoire")
        @Future(message = "Le rendez-vous doit être dans le futur")
        Instant scheduledAt,
        @Size(max = 255)
        String reason
) {
}
