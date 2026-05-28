package com.afya.platform.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BedRequest(
        @NotBlank(message = "Le libellé du lit est obligatoire")
        @Size(max = 40)
        String label
) {
}
