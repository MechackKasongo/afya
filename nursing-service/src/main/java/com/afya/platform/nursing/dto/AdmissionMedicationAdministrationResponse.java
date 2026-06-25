package com.afya.platform.nursing.dto;

import com.afya.platform.nursing.model.VitalSignSlot;

import java.time.LocalDate;

public record AdmissionMedicationAdministrationResponse(
        Long id,
        Long prescriptionLineId,
        LocalDate administrationDate,
        VitalSignSlot slot,
        boolean administered
) {
}
