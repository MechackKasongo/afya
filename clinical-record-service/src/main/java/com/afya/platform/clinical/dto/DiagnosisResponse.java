package com.afya.platform.clinical.dto;

import java.time.Instant;

public record DiagnosisResponse(
        Long id,
        String code,
        String label,
        Instant recordedAt,
        String authorUsername
) {
}
