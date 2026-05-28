package com.afya.platform.bff.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        List<String> roles,
        boolean active,
        List<Long> hospitalServiceIds,
        String generatedPassword
) {
}
