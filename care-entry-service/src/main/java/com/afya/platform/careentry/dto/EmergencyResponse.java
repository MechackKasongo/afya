package com.afya.platform.careentry.dto;

import java.time.Instant;

public record EmergencyResponse(
        Long id,
        Long patientId,
        String patientName,
        Instant arrivedAt,
        Instant endedAt,
        String status,
        String triageNotes,
        String priority,
        String triageLevel,
        String orientation
) {
}
