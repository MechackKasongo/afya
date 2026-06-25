package com.afya.platform.admission.stay.dto;

import java.time.Instant;

public record StayResponse(
        Long id,
        Long patientId,
        String patientName,
        String dossierNumber,
        Long admissionId,
        Instant checkInAt,
        Instant checkOutAt,
        String roomLabel,
        String bedLabel,
        String status
) {
}
