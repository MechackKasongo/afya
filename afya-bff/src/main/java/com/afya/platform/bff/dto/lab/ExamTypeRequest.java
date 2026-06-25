package com.afya.platform.bff.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull ExamCategory category,
        String parameters
) {
}
