package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.ActivityReportResponse;
import com.afya.platform.bff.dto.OperationalStatsResponse;
import com.afya.platform.bff.support.ActivityReportFallback;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.DownstreamErrors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class ReportClient {

    private final RestClient restClient;

    public ReportClient(
            @Value("${app.services.report-base-url}") String baseUrl,
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

    public OperationalStatsResponse operationalStats(Instant from, Instant to, String authorizationHeader) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/reports/operational-stats");
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
                .body(OperationalStatsResponse.class);
    }

    public ResponseEntity<byte[]> exportActivity(
            Instant from,
            Instant to,
            String format,
            String authorizationHeader
    ) {
        return restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/reports/activity/export")
                            .queryParam("format", format);
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
                .toEntity(byte[].class);
    }
}
