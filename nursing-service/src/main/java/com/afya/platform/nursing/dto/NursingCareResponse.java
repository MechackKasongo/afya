package com.afya.platform.nursing.dto;

import java.time.Instant;

public record NursingCareResponse(
        Long id,
        String careType,
        Instant performedAt,
        String nurseUsername,
        String description
) {
}
