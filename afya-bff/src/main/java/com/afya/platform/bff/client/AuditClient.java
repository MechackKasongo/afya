package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.ActivityReportResponse;
import com.afya.platform.bff.dto.AuditEventResponse;
import com.afya.platform.bff.support.ActivityReportFallback;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.DownstreamErrors;
import com.afya.platform.bff.support.PageRestSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class AuditClient {

    private final RestClient restClient;

    public AuditClient(
            @Value("${app.services.audit-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public ActivityReportResponse activityReport(Instant from, Instant to, String authorizationHeader) {
        try {
            ActivityReportResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/v1/reports/activity");
                        if (from != null) {
                            builder.queryParam("from", from);
                        }
                        if (to != null) {
                            builder.queryParam("to", to);
                        }
                        return builder.build();
                    })
                    .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                    .retrieve()
                    .body(ActivityReportResponse.class);
            if (response == null) {
                return ActivityReportFallback.empty(from, to);
            }
            return response;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                return ActivityReportFallback.empty(from, to);
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (DownstreamErrors.isUnreachable(ex)) {
                return ActivityReportFallback.empty(from, to);
            }
            throw ex;
        }
    }

    public Page<AuditEventResponse> searchEvents(
            String action,
            String resourceType,
            String actorUsername,
            String sourceService,
            String resource,
            Instant from,
            Instant to,
            String sortBy,
            String sortDir,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/audit/events");
                    if (action != null && !action.isBlank()) {
                        builder.queryParam("action", action);
                    }
                    if (resourceType != null && !resourceType.isBlank()) {
                        builder.queryParam("resourceType", resourceType);
                    }
                    if (actorUsername != null && !actorUsername.isBlank()) {
                        builder.queryParam("actorUsername", actorUsername);
                    }
                    if (sourceService != null && !sourceService.isBlank()) {
                        builder.queryParam("sourceService", sourceService);
                    }
                    if (resource != null && !resource.isBlank()) {
                        builder.queryParam("resource", resource);
                    }
                    if (from != null) {
                        builder.queryParam("from", from);
                    }
                    if (to != null) {
                        builder.queryParam("to", to);
                    }
                    if (sortBy != null && !sortBy.isBlank()) {
                        builder.queryParam("sortBy", sortBy);
                    }
                    if (sortDir != null && !sortDir.isBlank()) {
                        builder.queryParam("sortDir", sortDir);
                    }
                    if (page != null) {
                        builder.queryParam("page", page);
                    }
                    if (size != null) {
                        builder.queryParam("size", size);
                    }
                    return builder.build();
                },
                AuditEventResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }
}
