package com.afya.platform.bff.dto.lab;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExamResultRequest(
        @NotNull Long labTechnicianId,
        String annotation,
        @NotEmpty List<@Valid ResultParameterRequest> parameters
) {
}
