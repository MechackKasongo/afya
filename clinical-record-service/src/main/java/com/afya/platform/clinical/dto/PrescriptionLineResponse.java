package com.afya.platform.clinical.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PrescriptionLineResponse(
        Long id,
        String drugName,
        String dosage,
        String frequency,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String prescribedBy,
        Instant createdAt,
        boolean administered
) {
}
