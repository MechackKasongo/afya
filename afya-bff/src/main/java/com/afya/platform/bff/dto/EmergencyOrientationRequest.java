package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmergencyOrientationRequest(
        @NotBlank @Size(max = 120) String orientation
) {
}
