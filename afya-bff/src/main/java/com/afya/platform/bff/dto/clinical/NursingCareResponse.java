package com.afya.platform.bff.dto.clinical;

import java.time.Instant;

public record NursingCareResponse(
        Long id,
        String careType,
        Instant performedAt,
        String nurseUsername,
        String description
) {
}
