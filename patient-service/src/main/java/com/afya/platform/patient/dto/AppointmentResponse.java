package com.afya.platform.patient.dto;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Instant scheduledAt,
        String status,
        String reason
) {
}
