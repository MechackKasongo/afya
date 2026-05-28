package com.afya.platform.catalog.dto;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        boolean active
) {
}
