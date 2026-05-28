package com.afya.platform.catalog.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestJwtFactory {

    private static final String SECRET =
            "test-access-secret-at-least-64-characters-long-for-hs512-signing-key-xx";

    private TestJwtFactory() {
    }

    public static String adminToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject("admin")
                .claim("fullName", "Administrateur test")
                .claim("roles", List.of("ADMIN"))
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
