package com.afya.platform.identity.service;

import com.afya.platform.identity.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final int MIN_UTF8_BYTES_HS512 = 64;

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtService(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.jwt.access-expiration-seconds:3600}") long accessExpirationSeconds,
            @Value("${app.jwt.refresh-expiration-seconds:2592000}") long refreshExpirationSeconds
    ) {
        this.accessSecretKey = Keys.hmacShaKeyFor(requireSecret(accessSecret).getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(requireSecret(refreshSecret).getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toList());
        List<Long> hospitalServiceIds = user.getHospitalServiceIds().stream().sorted().collect(Collectors.toList());
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .claim("fullName", user.getFullName())
                .claim("roles", roles)
                .claim("hospitalServiceIds", hospitalServiceIds)
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(accessSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String issueRefreshToken(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshExpirationSeconds)))
                .signWith(refreshSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token, accessSecretKey);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Jeton d'accès attendu");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token, refreshSecretKey);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Jeton refresh attendu");
        }
        return claims;
    }

    public Instant getExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    private Claims parse(String token, SecretKey key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static String requireSecret(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Secret JWT manquant");
        }
        String secret = raw.strip();
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_UTF8_BYTES_HS512) {
            throw new IllegalStateException("Secret JWT trop court pour HS512 (min 64 octets UTF-8)");
        }
        return secret;
    }
}
