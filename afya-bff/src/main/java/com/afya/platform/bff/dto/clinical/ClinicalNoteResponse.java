package com.afya.platform.bff.dto.clinical;

import java.time.Instant;

public record ClinicalNoteResponse(
        Long id,
        Instant authoredAt,
        String authorUsername,
        String narrative
) {
}
