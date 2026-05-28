package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyTriageRequest(
        @NotBlank @Size(max = 20) String triageLevel,
        @Size(max = 500) String details
) {
}
