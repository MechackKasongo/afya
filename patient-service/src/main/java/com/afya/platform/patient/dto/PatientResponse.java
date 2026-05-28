package com.afya.platform.patient.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        String postName,
        String dossierNumber,
        LocalDate birthDate,
        String sex,
        String phone,
        String email,
        String address,
        String bloodGroup,
        BigDecimal heightCm,
        Instant deceasedAt
) {
}
