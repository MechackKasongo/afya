package com.afya.platform.patient.dto;

import com.afya.platform.patient.model.AntecedentType;

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
