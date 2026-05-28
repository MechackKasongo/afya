package com.afya.platform.bff.dto;

import java.time.Instant;

public record ConsultationResponse(
        Long id,
        Long patientId,
        Long admissionId,
        String doctorName,
        String reason,
        Instant consultationDateTime
) {
}
