package com.afya.platform.clinical.dto;

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
