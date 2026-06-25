package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

public record DischargeRequest(
        DischargeType type,
        @Size(max = 2000) String postDischargeInstructions,
        @Size(max = 255) String reason
) {
    public DischargeRequest(String reason) {
        this(null, null, reason);
    }
}
