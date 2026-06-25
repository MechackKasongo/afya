package com.afya.platform.hospital.dto;

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
