package com.afya.platform.lab.dto;

import com.afya.platform.lab.model.ExamRequestStatus;
import com.afya.platform.lab.model.ExamUrgency;

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
        String postponeReason,
        List<ExamTypeSummary> examTypes
) {
}
