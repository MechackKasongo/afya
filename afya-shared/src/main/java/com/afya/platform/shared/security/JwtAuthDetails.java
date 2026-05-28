package com.afya.platform.shared.security;

import java.util.List;

/** Détails d'authentification JWT (périmètre hospitalier, etc.). */
public record JwtAuthDetails(List<String> roles, List<Long> hospitalServiceIds) {
}
