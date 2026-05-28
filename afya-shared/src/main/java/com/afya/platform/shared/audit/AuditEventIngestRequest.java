package com.afya.platform.shared.audit;

public record AuditEventIngestRequest(
        String actorUsername,
        String action,
        String resourceType,
        String resourceId,
        String sourceService,
        String metadataJson
) {
}
