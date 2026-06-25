package com.afya.platform.user.dto;

import java.util.List;

public record InternalAuthProfileResponse(
        Long id,
        String username,
        String fullName,
        String passwordHash,
        boolean active,
        List<String> roles,
        List<Long> hospitalServiceIds
) {
}
