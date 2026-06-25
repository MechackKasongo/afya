package com.afya.platform.hospital.dto;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        boolean active
) {
}
