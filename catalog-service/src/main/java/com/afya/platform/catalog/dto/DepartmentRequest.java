package com.afya.platform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @Size(max = 40)
        String code,
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 120)
        String name,
        Boolean active
) {
}
