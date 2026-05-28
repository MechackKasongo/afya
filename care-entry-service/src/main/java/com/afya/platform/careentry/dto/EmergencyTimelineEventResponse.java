package com.afya.platform.careentry.dto;

import java.time.Instant;

public record EmergencyTimelineEventResponse(
        Long id,
        Long urgenceId,
        String type,
        String details,
        Instant createdAt
) {
}
