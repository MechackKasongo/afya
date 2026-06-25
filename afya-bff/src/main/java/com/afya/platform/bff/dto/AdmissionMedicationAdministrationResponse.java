package com.afya.platform.bff.dto;

import java.time.LocalDate;

public record AdmissionMedicationAdministrationResponse(
        Long id,
        Long prescriptionLineId,
        LocalDate administrationDate,
        String slot,
        boolean administered
) {
}
