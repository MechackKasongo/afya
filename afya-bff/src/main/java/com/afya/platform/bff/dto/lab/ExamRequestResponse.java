package com.afya.platform.bff.dto.lab;

import java.time.Instant;
import java.util.List;

public record ExamRequestResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long admissionId,
        Instant requestedAt,
        ExamUrgency urgency,
        ExamRequestStatus status,
        String comment,
        List<ExamTypeSummary> examTypes
) {
}
