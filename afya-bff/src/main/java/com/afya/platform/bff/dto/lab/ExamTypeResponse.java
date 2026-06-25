package com.afya.platform.bff.dto.lab;

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
