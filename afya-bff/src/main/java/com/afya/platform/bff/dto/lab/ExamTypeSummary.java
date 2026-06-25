package com.afya.platform.bff.dto.lab;

public record ExamTypeSummary(
        Long id,
        String name,
        ExamCategory category
) {
}
