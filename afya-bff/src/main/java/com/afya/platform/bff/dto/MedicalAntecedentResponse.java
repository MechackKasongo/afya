package com.afya.platform.bff.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MedicalAntecedentResponse(
        Long id,
        Long patientId,
        AntecedentType type,
        String description,
        LocalDate eventDate,
        Instant createdAt
) {
}
