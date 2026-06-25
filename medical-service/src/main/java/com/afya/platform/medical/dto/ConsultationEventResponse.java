package com.afya.platform.medical.dto;

import java.time.Instant;

public record ConsultationEventResponse(
        Long id,
        Long consultationId,
        Long patientId,
        String type,
        String content,
        String diseaseType,
        String diseaseName,
        Long examRequestId,
        Instant createdAt
) {
}
