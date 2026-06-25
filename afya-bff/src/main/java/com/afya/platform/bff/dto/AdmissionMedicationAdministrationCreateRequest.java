package com.afya.platform.bff.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdmissionMedicationAdministrationCreateRequest(
        @NotNull LocalDate administrationDate,
        @NotNull String slot,
        @NotNull Boolean administered
) {
}
