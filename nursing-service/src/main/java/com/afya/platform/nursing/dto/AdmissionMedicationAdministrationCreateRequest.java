package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.VitalSignSlot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdmissionMedicationAdministrationCreateRequest(
        @NotNull LocalDate administrationDate,
        @NotNull VitalSignSlot slot,
        @NotNull Boolean administered
) {
}
