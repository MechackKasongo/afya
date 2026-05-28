package com.afya.platform.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record AuditEventCreateRequest(
        UUID eventId,
        Instant occurredAt,
        @Size(max = 80) String actorUsername,
        @NotBlank @Size(max = 80) String action,
        @NotBlank @Size(max = 60) String resourceType,
        @Size(max = 80) String resourceId,
        @NotBlank @Size(max = 40) String sourceService,
        String metadataJson
) {
}
