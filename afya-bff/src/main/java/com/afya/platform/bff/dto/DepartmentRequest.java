package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        Boolean active
) {
}
