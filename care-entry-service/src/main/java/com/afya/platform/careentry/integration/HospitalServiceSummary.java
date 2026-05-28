package com.afya.platform.careentry.integration;

public record HospitalServiceSummary(
        Long id,
        String name,
        int bedCapacity,
        boolean active
) {
}
