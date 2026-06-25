package com.afya.platform.lab.dto;

import java.time.Instant;
import java.util.List;

public record ExamResultResponse(
        Long id,
        Long requestId,
        Long patientId,
        Long labTechnicianId,
        Instant resultedAt,
        String annotation,
        List<ResultParameterResponse> parameters
) {
}
