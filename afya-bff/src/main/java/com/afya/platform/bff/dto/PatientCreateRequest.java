package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientCreateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Size(max = 120) String postName,
        @Size(max = 40) String dossierNumber,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Size(max = 10) String sex,
        @Size(max = 120) String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 255) String address,
        @Size(max = 10) String bloodGroup,
        BigDecimal heightCm
) {
}
