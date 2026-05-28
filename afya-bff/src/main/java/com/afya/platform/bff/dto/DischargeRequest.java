package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

public record DischargeRequest(
        @Size(max = 255)
        String reason
) {
}
