package com.afya.platform.bff.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpecimenCollectionRequest(
        @NotNull Long labTechnicianId,
        @NotBlank String sampleType
) {
}
