package com.afya.platform.bff.support;

import com.afya.platform.bff.dto.ActivityReportResponse;

import java.time.Instant;
import java.util.List;

public final class ActivityReportFallback {

    private static final String NOTICE =
            "Le service d'audit est indisponible. Les statistiques affichées sont vides.";

    private ActivityReportFallback() {
    }

    public static ActivityReportResponse empty(Instant from, Instant to) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        return new ActivityReportResponse(
                rangeFrom,
                rangeTo,
                0L,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                true,
                NOTICE
        );
    }
}
