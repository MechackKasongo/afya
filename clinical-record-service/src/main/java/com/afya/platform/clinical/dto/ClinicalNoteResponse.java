package com.afya.platform.clinical.dto;

import java.time.Instant;

public record ClinicalNoteResponse(
        Long id,
        Instant authoredAt,
        String authorUsername,
        String narrative
) {
}
