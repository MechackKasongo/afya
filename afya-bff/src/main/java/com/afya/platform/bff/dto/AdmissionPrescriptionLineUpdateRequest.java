package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdmissionPrescriptionLineUpdateRequest(
        @NotBlank @Size(max = 120) String medicationName,
        @Size(max = 500) String dosageText,
        @Size(max = 80) String frequencyText,
        @Size(max = 500) String instructionsText,
        @Size(max = 80) String prescriberName,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull Boolean active
) {
}
