package com.afya.platform.shared.audit;

/**
 * Publication asynchrone d'événements vers audit-service (HTTP + clé d'ingestion).
 */
public interface AuditEventPublisher {

    void publish(
            String action,
            String resourceType,
            String resourceId,
            String actorUsername,
            String metadataJson
    );
}
