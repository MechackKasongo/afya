package com.afya.platform.bff.dto;

import java.time.Instant;

public record AdmissionResponse(
        Long id,
        Long patientId,
        String patientName,
        String dossierNumber,
        Long hospitalServiceId,
        String hospitalServiceName,
        Instant admittedAt,
        Instant dischargedAt,
        String status,
        String admissionReason,
        String dischargeReason
) {
}
