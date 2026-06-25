package com.afya.platform.lab.dto;

import com.afya.platform.lab.model.ExamCategory;

import java.time.Instant;

public record ExamTypeResponse(
        Long id,
        String name,
        String description,
        ExamCategory category,
        String parameters,
        boolean active,
        Instant createdAt
) {
}
