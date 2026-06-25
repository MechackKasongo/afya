package com.afya.platform.patient.dto;

import java.time.Instant;

public record EmergencyContactResponse(
        Long id,
        Long patientId,
        String firstName,
        String lastName,
        String relationship,
        String phone,
        Instant createdAt
) {
}
