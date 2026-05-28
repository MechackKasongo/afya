package com.afya.platform.identity.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.identity.dto.MeResponse;
import com.afya.platform.identity.dto.TokenResponse;
import com.afya.platform.identity.model.AppUser;
import com.afya.platform.identity.model.RefreshToken;
import com.afya.platform.identity.model.RevokedAccessJti;
import com.afya.platform.identity.repository.AppUserRepository;
import com.afya.platform.identity.repository.RefreshTokenRepository;
import com.afya.platform.identity.repository.RevokedAccessJtiRepository;
import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPublisher auditEventPublisher;

    public AuthService(
            AppUserRepository appUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessJtiRepository revokedAccessJtiRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuditEventPublisher auditEventPublisher
    ) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public TokenResponse login(String username, String password) {
        String normalizedUsername = username.strip();
        AppUser user = appUserRepository.findByUsernameIgnoreCase(normalizedUsername)
                .filter(AppUser::isActive)
                .orElse(null);
        if (user == null) {
            auditEventPublisher.publish("LOGIN_FAILED", "USER", normalizedUsername, normalizedUsername, null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditEventPublisher.publish("LOGIN_FAILED", "USER", user.getUsername(), user.getUsername(), null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        TokenResponse tokens = issueTokens(user);
        auditEventPublisher.publish(
                "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), user.getUsername(), null);
        return tokens;
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenRaw) {
        Claims claims = jwtService.parseRefreshToken(refreshTokenRaw);
        String hash = hashToken(refreshTokenRaw);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Jeton refresh invalide"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Jeton refresh expiré");
        }
        stored.setRevoked(true);
        AppUser user = stored.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Compte inactif");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(String username, HttpServletRequest request, String refreshTokenRaw, boolean revokeAllSessions) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (revokeAllSessions) {
            refreshTokenRepository.revokeAllForUser(user);
        } else if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshTokenRaw))
                    .ifPresent(token -> token.setRevoked(true));
        }
        String bearer = extractBearer(request);
        if (bearer != null) {
            Claims access = jwtService.parseAccessToken(bearer);
            revokedAccessJtiRepository.save(new RevokedAccessJti(access.getId(), jwtService.getExpiration(access)));
        }
        auditEventPublisher.publish("LOGOUT_SUCCESS", "USER", user.getUsername(), user.getUsername(), null);
    }

    public MeResponse me(String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        return toMe(user);
    }

    private TokenResponse issueTokens(AppUser user) {
        String access = jwtService.issueAccessToken(user);
        String refresh = jwtService.issueRefreshToken(user);
        persistRefresh(user, refresh);
        MeResponse me = toMe(user);
        return new TokenResponse(access, refresh, "Bearer", 3600, me);
    }

    private void persistRefresh(AppUser user, String refreshTokenRaw) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(refreshTokenRaw));
        Claims claims = jwtService.parseRefreshToken(refreshTokenRaw);
        token.setExpiresAt(jwtService.getExpiration(claims));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
    }

    private MeResponse toMe(AppUser user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> UserAdminService.toUiRole(r.getCode()))
                .sorted()
                .collect(Collectors.toList());
        List<Long> hospitalServiceIds = user.getHospitalServiceIds().stream().sorted().collect(Collectors.toList());
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                hospitalServiceIds,
                List.of());
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
