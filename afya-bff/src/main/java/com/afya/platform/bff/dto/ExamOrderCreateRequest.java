package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExamOrderCreateRequest(
        @NotNull Long doctorId,
        @NotEmpty List<Long> examTypeIds,
        ExamUrgency urgency,
        @Size(max = 4000) String content
) {
}
