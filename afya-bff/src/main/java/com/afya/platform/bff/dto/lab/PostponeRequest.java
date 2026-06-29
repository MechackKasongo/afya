package com.afya.platform.bff.dto.lab;

import jakarta.validation.constraints.Size;

public record PostponeRequest(
        @Size(max = 1000) String reason
) {
}
