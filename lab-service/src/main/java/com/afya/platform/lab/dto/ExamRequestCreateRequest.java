package com.afya.platform.lab.dto;

import com.afya.platform.lab.model.ExamUrgency;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExamRequestCreateRequest(
        @NotNull Long patientId,
        @NotNull Long doctorId,
        Long admissionId,
        @NotNull ExamUrgency urgency,
        String comment,
        @NotEmpty List<Long> examTypeIds
) {
}
