package com.afya.platform.admission.dto;

import java.time.Instant;

public record EmergencyTimelineEventResponse(
        Long id,
        Long urgenceId,
        String type,
        String details,
        Instant createdAt
) {
}
