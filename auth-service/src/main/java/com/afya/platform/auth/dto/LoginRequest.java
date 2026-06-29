package com.afya.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(max = 80, message = "Nom d'utilisateur trop long") String username,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(max = 200, message = "Mot de passe trop long") String password
) {
}
