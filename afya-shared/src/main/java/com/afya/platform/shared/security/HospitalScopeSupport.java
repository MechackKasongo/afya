package com.afya.platform.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

public final class HospitalScopeSupport {

    private HospitalScopeSupport() {
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public static Optional<List<Long>> currentHospitalServiceIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof JwtAuthDetails details)) {
            return Optional.empty();
        }
        List<Long> ids = details.hospitalServiceIds();
        if (ids == null || ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(ids));
    }
}
