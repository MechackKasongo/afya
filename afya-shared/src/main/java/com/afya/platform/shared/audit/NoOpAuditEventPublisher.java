package com.afya.platform.shared.audit;

public final class NoOpAuditEventPublisher implements AuditEventPublisher {

    @Override
    public void publish(
            String action,
            String resourceType,
            String resourceId,
            String actorUsername,
            String metadataJson) {
    }
}
