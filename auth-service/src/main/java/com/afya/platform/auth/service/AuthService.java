package com.afya.platform.auth.service;

import com.afya.platform.auth.dto.MeResponse;
import com.afya.platform.auth.dto.TokenResponse;
import com.afya.platform.auth.integration.AuthUserProfile;
import com.afya.platform.auth.integration.UserServiceClient;
import com.afya.platform.auth.model.RefreshToken;
import com.afya.platform.auth.model.RevokedAccessJti;
import com.afya.platform.auth.repository.RefreshTokenRepository;
import com.afya.platform.auth.repository.RevokedAccessJtiRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditEventPublisher;

    public AuthService(
            UserServiceClient userServiceClient,
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessJtiRepository revokedAccessJtiRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuditEventPublisher auditEventPublisher
    ) {
        this.userServiceClient = userServiceClient;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public TokenResponse login(String username, String password) {
        String normalizedUsername = username.strip();
        AuthUserProfile user = findActiveUser(normalizedUsername);
        if (user == null) {
            auditEventPublisher.publish("LOGIN_FAILED", "USER", normalizedUsername, normalizedUsername, null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            auditEventPublisher.publish("LOGIN_FAILED", "USER", user.username(), user.username(), null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        TokenResponse tokens = issueTokens(user);
        auditEventPublisher.publish(
                "LOGIN_SUCCESS", "USER", String.valueOf(user.id()), user.username(), null);
        return tokens;
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenRaw) {
        jwtService.parseRefreshToken(refreshTokenRaw);
        String hash = hashToken(refreshTokenRaw);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Jeton refresh invalide"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Jeton refresh expiré");
        }
        stored.setRevoked(true);
        AuthUserProfile user = userServiceClient.findById(stored.getUserId());
        if (!user.active()) {
            throw new UnauthorizedException("Compte inactif");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String username, HttpServletRequest request, String refreshTokenRaw, boolean revokeAllSessions) {
        AuthUserProfile user = userServiceClient.findByUsername(username);
        if (revokeAllSessions) {
            refreshTokenRepository.revokeAllForUser(user.id());
        } else if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshTokenRaw))
                    .ifPresent(token -> token.setRevoked(true));
        }
        String bearer = extractBearer(request);
        if (bearer != null) {
            Claims access = jwtService.parseAccessToken(bearer);
            revokedAccessJtiRepository.save(new RevokedAccessJti(access.getId(), jwtService.getExpiration(access)));
        }
        auditEventPublisher.publish("LOGOUT_SUCCESS", "USER", user.username(), user.username(), null);
    }

    public MeResponse me(String username) {
        return toMe(userServiceClient.findByUsername(username));
    }

    private AuthUserProfile findActiveUser(String username) {
        try {
            AuthUserProfile user = userServiceClient.findByUsername(username);
            return user.active() ? user : null;
        } catch (NotFoundException ex) {
            return null;
        }
    }

    private TokenResponse issueTokens(AuthUserProfile user) {
        String access = jwtService.issueAccessToken(user);
        String refresh = jwtService.issueRefreshToken(user);
        persistRefresh(user.id(), refresh);
        return new TokenResponse(access, refresh, "Bearer", 3600, toMe(user));
    }

    private void persistRefresh(Long userId, String refreshTokenRaw) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hashToken(refreshTokenRaw));
        Claims claims = jwtService.parseRefreshToken(refreshTokenRaw);
        token.setExpiresAt(jwtService.getExpiration(claims));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
    }

    private MeResponse toMe(AuthUserProfile user) {
        List<String> roles = user.roles().stream()
                .map(AuthService::toUiRole)
                .sorted()
                .collect(Collectors.toList());
        List<Long> hospitalServiceIds = user.hospitalServiceIds().stream().sorted().collect(Collectors.toList());
        return new MeResponse(
                user.id(),
                user.username(),
                user.fullName(),
                roles,
                hospitalServiceIds,
                List.of());
    }

    static String toUiRole(String code) {
        return "ROLE_" + code;
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7).trim();
    }

    static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
