package com.afya.platform.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAccessTokenValidator {

    private static final int MIN_UTF8_BYTES_HS512 = 64;

    private final SecretKey accessSecretKey;

    public JwtAccessTokenValidator(@Value("${app.jwt.access-secret}") String accessSecret) {
        String secret = requireSecret(accessSecret);
        this.accessSecretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Jeton d'accès attendu");
        }
        return claims;
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Long> hospitalServiceIds(Claims claims) {
        List<Number> raw = claims.get("hospitalServiceIds", List.class);
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(Number::longValue).toList();
    }

    private static String requireSecret(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Secret JWT manquant");
        }
        String secret = raw.strip();
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_UTF8_BYTES_HS512) {
            throw new IllegalStateException("Secret JWT trop court pour HS512");
        }
        return secret;
    }
}
