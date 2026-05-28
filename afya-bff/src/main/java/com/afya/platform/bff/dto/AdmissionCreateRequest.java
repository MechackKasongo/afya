package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AdmissionCreateRequest(
        @NotNull Long patientId,
        @NotNull Long hospitalServiceId,
        Instant admittedAt,
        String roomLabel,
        String bedLabel,
        String admissionReason
) {
}
