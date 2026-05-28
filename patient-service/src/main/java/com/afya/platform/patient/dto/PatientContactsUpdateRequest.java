package com.afya.platform.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PatientContactsUpdateRequest(
        @Size(max = 120)
        String phone,
        @Email(message = "Email invalide")
        @Size(max = 120)
        String email,
        @Size(max = 255)
        String address
) {
}
