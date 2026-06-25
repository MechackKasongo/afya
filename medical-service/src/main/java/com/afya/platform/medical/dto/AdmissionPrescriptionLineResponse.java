package com.afya.platform.medical.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AdmissionPrescriptionLineResponse(
        Long id,
        Long admissionId,
        String medicationName,
        String dosageText,
        String frequencyText,
        String instructionsText,
        String prescriberName,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt
) {
}
