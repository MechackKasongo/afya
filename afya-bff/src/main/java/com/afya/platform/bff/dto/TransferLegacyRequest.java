package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferLegacyRequest(
        @NotBlank @Size(max = 120) String toService,
        @Size(max = 40) String room,
        @Size(max = 40) String bed,
        @Size(max = 255) String note
) {
}
