package com.afya.platform.auth.integration;

import java.util.List;

public record AuthUserProfile(
        Long id,
        String username,
        String fullName,
        boolean active,
        List<String> roles,
        List<Long> hospitalServiceIds
) {
}
