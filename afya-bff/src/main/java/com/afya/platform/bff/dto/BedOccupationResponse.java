package com.afya.platform.bff.dto;

import java.time.Instant;

public record BedOccupationResponse(
        Long id,
        Long bedId,
        String bedLabel,
        Long patientId,
        Long admissionId,
        Instant startedAt,
        Instant endedAt
) {
}
