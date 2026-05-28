package com.afya.platform.shared.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestClient;

public class RestAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestAuditEventPublisher.class);
    static final String INGESTION_HEADER = "X-Audit-Ingestion-Key";

    private final RestClient restClient;
    private final String ingestionKey;
    private final String sourceService;

    public RestAuditEventPublisher(String baseUrl, String ingestionKey, String sourceService) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.ingestionKey = ingestionKey.strip();
        this.sourceService = sourceService.strip();
    }

    @Override
    @Async
    public void publish(
            String action,
            String resourceType,
            String resourceId,
            String actorUsername,
            String metadataJson) {
        String enrichedMetadata = AuditMetadata.enrichWithActorRoles(metadataJson);
        try {
            restClient.post()
                    .uri("/api/v1/audit/events")
                    .header(INGESTION_HEADER, ingestionKey)
                    .body(new AuditEventIngestRequest(
                            actorUsername,
                            action,
                            resourceType,
                            resourceId,
                            sourceService,
                            enrichedMetadata))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Publication audit échouée (action={}, actor={}): {}", action, actorUsername, ex.getMessage());
        }
    }
}
