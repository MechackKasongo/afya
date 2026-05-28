package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

public record DeathDeclarationRequest(
        @Size(max = 500)
        String note
) {
}
