package com.afya.platform.admission.stay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StayRelocateRequest(
        @NotBlank @Size(max = 40) String roomLabel,
        @NotBlank @Size(max = 40) String bedLabel
) {
}
