package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @Size(max = 80) String username,
        @Size(max = 120) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 120) String postName,
        @Size(max = 255) String password,
        Integer generatedPasswordLength,
        Integer passwordVariation,
        @NotBlank @Size(max = 40) String role,
        List<Long> hospitalServiceIds
) {
}
