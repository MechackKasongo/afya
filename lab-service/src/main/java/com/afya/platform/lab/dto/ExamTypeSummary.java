package com.afya.platform.lab.dto;

import com.afya.platform.lab.model.ExamCategory;

public record ExamTypeSummary(
        Long id,
        String name,
        ExamCategory category
) {
}
