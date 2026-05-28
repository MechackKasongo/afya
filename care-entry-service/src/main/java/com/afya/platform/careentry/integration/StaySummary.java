package com.afya.platform.careentry.integration;

public record StaySummary(
        Long id,
        Long admissionId,
        String roomLabel,
        String bedLabel
) {
}
