package com.afya.platform.shared.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AuditPublisherConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true")
    @ConditionalOnMissingBean(AuditEventPublisher.class)
    public AuditEventPublisher auditEventPublisher(
            @Value("${app.services.audit-base-url}") String baseUrl,
            @Value("${app.audit.ingestion-key}") String ingestionKey,
            @Value("${app.audit.source-service:${spring.application.name}}") String sourceService) {
        return new RestAuditEventPublisher(baseUrl, ingestionKey, sourceService);
    }

    @Bean
    @ConditionalOnProperty(name = "app.audit.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(AuditEventPublisher.class)
    public AuditEventPublisher noOpAuditEventPublisher() {
        return new NoOpAuditEventPublisher();
    }
}
