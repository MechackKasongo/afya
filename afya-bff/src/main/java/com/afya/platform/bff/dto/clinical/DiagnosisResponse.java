package com.afya.platform.bff.dto.clinical;

import java.time.Instant;

public record DiagnosisResponse(
        Long id,
        String code,
        String label,
        Instant recordedAt,
        String authorUsername
) {
}
