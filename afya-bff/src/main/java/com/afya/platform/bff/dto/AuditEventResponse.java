package com.afya.platform.bff.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        Long id,
        UUID eventId,
        Instant occurredAt,
        String actorUsername,
        String action,
        String resourceType,
        String resourceId,
        String sourceService,
        String metadataJson,
        Instant createdAt
) {
}
