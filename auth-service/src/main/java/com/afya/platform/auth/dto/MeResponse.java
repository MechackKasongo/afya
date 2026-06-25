package com.afya.platform.auth.dto;

import java.util.List;

public record MeResponse(
        Long id,
        String username,
        String fullName,
        List<String> roles,
        List<Long> hospitalServiceIds,
        List<String> hospitalServiceNames
) {
}
