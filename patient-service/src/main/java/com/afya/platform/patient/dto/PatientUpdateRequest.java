package com.afya.platform.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientUpdateRequest(
        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 80)
        String firstName,
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 80)
        String lastName,
        @Size(max = 120)
        String postName,
        @NotNull(message = "La date de naissance est obligatoire")
        @Past(message = "La date de naissance doit être dans le passé")
        LocalDate birthDate,
        @NotBlank(message = "Le sexe est obligatoire")
        @Size(max = 10)
        String sex,
        @Size(max = 120)
        String phone,
        @Email(message = "Email invalide")
        @Size(max = 120)
        String email,
        @Size(max = 255)
        String address,
        @Size(max = 10)
        String bloodGroup,
        BigDecimal heightCm
) {
}
