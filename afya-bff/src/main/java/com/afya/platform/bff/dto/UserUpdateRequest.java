package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 40) String role,
        @Size(max = 255) String password,
        List<Long> hospitalServiceIds
) {
}
