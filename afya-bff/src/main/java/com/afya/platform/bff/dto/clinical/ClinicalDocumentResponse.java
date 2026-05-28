package com.afya.platform.bff.dto.clinical;

import java.time.Instant;

public record ClinicalDocumentResponse(
        Long id,
        String title,
        String contentType,
        String objectStorageKey,
        Instant uploadedAt,
        String uploadedBy
) {
}
