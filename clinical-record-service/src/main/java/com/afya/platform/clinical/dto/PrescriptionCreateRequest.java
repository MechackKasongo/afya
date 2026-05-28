package com.afya.platform.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionCreateRequest(
        @NotBlank @Size(max = 120) String drugName,
        @NotBlank @Size(max = 500) String prescriptionDetails
) {
}
