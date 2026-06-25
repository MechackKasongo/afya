package com.afya.platform.lab.dto;

import com.afya.platform.lab.model.ExamCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamTypeRequest(
        @NotBlank String name,
        String description,
        @NotNull ExamCategory category,
        String parameters
) {
}
