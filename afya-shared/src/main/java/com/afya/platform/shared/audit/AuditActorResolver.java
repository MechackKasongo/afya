package com.afya.platform.shared.audit;

import com.afya.platform.shared.security.JwtAuthDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class AuditActorResolver {

    private AuditActorResolver() {
    }

    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String username) {
            return username;
        }
        return "unknown";
    }

    public static List<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return details.roles() != null ? details.roles() : List.of();
        }
        return List.of();
    }
}
