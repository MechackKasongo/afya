package com.afya.platform.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 120) String diseaseType,
        @Size(max = 255) String diseaseName
) {
}
