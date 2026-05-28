package com.afya.platform.stay.integration;

import java.time.Instant;

public record AdmissionSummary(
        Long id,
        Long patientId,
        Long hospitalServiceId,
        Instant admittedAt,
        String status
) {
}
