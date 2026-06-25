package com.afya.platform.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpecimenCollectionRequest(
        @NotNull Long labTechnicianId,
        @NotBlank String sampleType
) {
}
