package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicalAntecedentCreateRequest(
        @NotNull AntecedentType type,
        @NotBlank @Size(max = 2000) String description,
        LocalDate eventDate
) {
}
