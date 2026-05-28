package com.afya.platform.clinical.dto;

import java.time.Instant;

public record ConsultationEventResponse(
        Long id,
        Long consultationId,
        Long patientId,
        String type,
        String content,
        String diseaseType,
        String diseaseName,
        Instant createdAt
) {
}
