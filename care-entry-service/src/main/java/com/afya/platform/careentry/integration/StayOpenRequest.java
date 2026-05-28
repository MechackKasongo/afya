package com.afya.platform.careentry.integration;

import java.time.Instant;

public record StayOpenRequest(
        Long admissionId,
        Long patientId,
        Instant checkInAt,
        String roomLabel,
        String bedLabel
) {
}
