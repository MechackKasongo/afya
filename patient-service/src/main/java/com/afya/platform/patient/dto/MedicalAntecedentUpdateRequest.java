package com.afya.platform.patient.dto;

import com.afya.platform.patient.model.AntecedentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MedicalAntecedentUpdateRequest(
        @NotNull AntecedentType type,
        @NotBlank @Size(max = 2000) String description,
        LocalDate eventDate
) {
}
