package com.afya.platform.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyOrientationRequest(
        @NotBlank @Size(max = 120) String orientation
) {
}
