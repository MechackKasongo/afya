package com.afya.platform.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NursingCareRequest(
        @NotBlank @Size(max = 80) String careType,
        @NotBlank @Size(max = 2000) String description
) {
}
