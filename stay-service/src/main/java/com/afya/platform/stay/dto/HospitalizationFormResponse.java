package com.afya.platform.stay.dto;

import java.time.Instant;

public record HospitalizationFormResponse(
        Long id,
        Long admissionId,
        Long stayId,
        String antecedentsText,
        String anamnesisText,
        String physicalExamPulmonaryText,
        String physicalExamCardiacText,
        String physicalExamAbdominalText,
        String physicalExamNeurologicalText,
        String physicalExamMiscText,
        String paraclinicalText,
        String conclusionText,
        Instant updatedAt
) {
}
