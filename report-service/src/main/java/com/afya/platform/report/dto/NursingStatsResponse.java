package com.afya.platform.report.dto;

import java.time.Instant;

public record NursingStatsResponse(
        Instant from,
        Instant to,
        long vitalSignReadings,
        long vitalSignAlerts,
        long prescriptionNotifications,
        long executedPrescriptions,
        boolean degraded,
        String notice
) {
}
