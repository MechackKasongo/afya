package com.afya.platform.identity.dto;

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
    public static UserResponse withoutPassword(
            Long id,
            String username,
            String email,
            String fullName,
            List<String> roles,
            boolean active,
            List<Long> hospitalServiceIds
    ) {
        return new UserResponse(id, username, email, fullName, roles, active, hospitalServiceIds, null);
    }
}
