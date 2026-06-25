package com.afya.platform.bff.dto.lab;

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
