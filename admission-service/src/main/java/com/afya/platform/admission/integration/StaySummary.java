package com.afya.platform.admission.integration;

public record StaySummary(
        Long id,
        Long admissionId,
        String roomLabel,
        String bedLabel
) {
}
