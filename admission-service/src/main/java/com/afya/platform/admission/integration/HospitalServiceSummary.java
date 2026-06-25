package com.afya.platform.admission.integration;

public record HospitalServiceSummary(
        Long id,
        String name,
        int bedCapacity,
        boolean active
) {
}
