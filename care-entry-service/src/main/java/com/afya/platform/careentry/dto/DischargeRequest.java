package com.afya.platform.careentry.dto;

import jakarta.validation.constraints.Size;

public record DischargeRequest(
        @Size(max = 255)
        String reason
) {
}
