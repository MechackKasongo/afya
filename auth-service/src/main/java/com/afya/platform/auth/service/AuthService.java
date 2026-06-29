package com.afya.platform.auth.service;

import com.afya.platform.auth.dto.MeResponse;
import com.afya.platform.auth.dto.TokenResponse;
import com.afya.platform.auth.integration.AuthUserProfile;
import com.afya.platform.auth.integration.UserServiceClient;
import com.afya.platform.auth.model.Credential;
import com.afya.platform.auth.model.CredentialStatus;
import com.afya.platform.auth.model.IssuedToken;
import com.afya.platform.auth.model.LoginJournalEntry;
import com.afya.platform.auth.model.LoginOutcome;
import com.afya.platform.auth.model.RefreshToken;
import com.afya.platform.auth.model.RevokedAccessJti;
import com.afya.platform.auth.repository.IssuedTokenRepository;
import com.afya.platform.auth.repository.LoginJournalRepository;
import com.afya.platform.auth.repository.RefreshTokenRepository;
import com.afya.platform.auth.repository.RevokedAccessJtiRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.exception.TooManyRequestsException;
import com.afya.platform.shared.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final CredentialService credentialService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;
    private final IssuedTokenRepository issuedTokenRepository;
    private final LoginJournalRepository loginJournalRepository;
    private final JwtService jwtService;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Le verrouillage par credential ({@link CredentialService}) protège déjà les comptes
     * <em>existants</em> contre la force brute (statut {@code BLOQUE} après N échecs). Ce throttle
     * complémentaire couvre le seul angle mort : le martèlement de <em>usernames inconnus</em>
     * (probing/énumération), avec une fenêtre glissante qui se lève automatiquement.
     */
    private static final Set<LoginOutcome> COUNTED_LOGIN_FAILURES =
            Set.of(LoginOutcome.FAILURE_USER_NOT_FOUND);

    private final int maxFailedAttempts;
    private final Duration lockWindow;

    public AuthService(
            UserServiceClient userServiceClient,
            CredentialService credentialService,
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessJtiRepository revokedAccessJtiRepository,
            IssuedTokenRepository issuedTokenRepository,
            LoginJournalRepository loginJournalRepository,
            JwtService jwtService,
            AuditEventPublisher auditEventPublisher,
            @Value("${app.auth.max-failed-login-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lock-window:15m}") Duration lockWindow
    ) {
        this.userServiceClient = userServiceClient;
        this.credentialService = credentialService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
        this.issuedTokenRepository = issuedTokenRepository;
        this.loginJournalRepository = loginJournalRepository;
        this.jwtService = jwtService;
        this.auditEventPublisher = auditEventPublisher;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockWindow = lockWindow;
    }

    @Transactional
    public TokenResponse login(String username, String password, String ipAddress) {
        String normalizedUsername = username.strip();
        enforceBruteForceProtection(normalizedUsername, ipAddress);
        Credential credential = credentialService.findByUsername(normalizedUsername).orElse(null);

        if (credential == null) {
            journalLogin(normalizedUsername, null, LoginOutcome.FAILURE_USER_NOT_FOUND, ipAddress);
            auditEventPublisher.publish("LOGIN_FAILED", "USER", normalizedUsername, normalizedUsername, null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        if (credential.getStatus() == CredentialStatus.BLOQUE) {
            journalLogin(credential.getUsername(), credential.getUserId(), LoginOutcome.FAILURE_ACCOUNT_LOCKED, ipAddress);
            auditEventPublisher.publish("LOGIN_FAILED", "USER", credential.getUsername(), credential.getUsername(), null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        if (!credentialService.verifyPassword(credential, password)) {
            journalLogin(credential.getUsername(), credential.getUserId(), LoginOutcome.FAILURE_BAD_PASSWORD, ipAddress);
            auditEventPublisher.publish("LOGIN_FAILED", "USER", credential.getUsername(), credential.getUsername(), null);
            throw new UnauthorizedException("Identifiants invalides");
        }
        AuthUserProfile user = findActiveUser(normalizedUsername);
        if (user == null) {
            journalLogin(credential.getUsername(), credential.getUserId(), LoginOutcome.FAILURE_ACCOUNT_INACTIVE, ipAddress);
            auditEventPublisher.publish("LOGIN_FAILED", "USER", credential.getUsername(), credential.getUsername(), null);
            throw new UnauthorizedException("Identifiants invalides");
        }

        TokenResponse tokens = issueTokens(user);
        journalLogin(user.username(), user.id(), LoginOutcome.SUCCESS, ipAddress);
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
            String jti = access.getId();
            Instant exp = jwtService.getExpiration(access);
            revokedAccessJtiRepository.save(new RevokedAccessJti(jti, exp));
            // Marquer l'IssuedToken correspondant comme révoqué
            issuedTokenRepository.findByJti(jti).ifPresent(IssuedToken::revoke);
        }
        auditEventPublisher.publish("LOGOUT_SUCCESS", "USER", user.username(), user.username(), null);
    }

    public MeResponse me(String username) {
        return toMe(userServiceClient.findByUsername(username));
    }

    // ── Helpers privés ─────────────────────────────────────────────────────────

    /**
     * Protection anti-force brute : si trop de tentatives infructueuses ont eu lieu pour ce
     * username dans la fenêtre glissante, on rejette la requête (HTTP 429) sans même vérifier
     * le mot de passe. Les rejets eux-mêmes ({@code FAILURE_ACCOUNT_LOCKED}) ne sont pas
     * comptabilisés, de sorte que le verrou se lève automatiquement une fois la fenêtre écoulée.
     */
    private void enforceBruteForceProtection(String username, String ipAddress) {
        if (maxFailedAttempts <= 0) {
            return;
        }
        Instant since = Instant.now().minus(lockWindow);
        long recentFailures = loginJournalRepository
                .countByUsernameIgnoreCaseAndOutcomeInAndOccurredAtAfter(username, COUNTED_LOGIN_FAILURES, since);
        if (recentFailures >= maxFailedAttempts) {
            journalLogin(username, null, LoginOutcome.FAILURE_ACCOUNT_LOCKED, ipAddress);
            auditEventPublisher.publish("LOGIN_THROTTLED", "USER", username, username, null);
            throw new TooManyRequestsException(
                    "Trop de tentatives de connexion infructueuses. Réessayez dans "
                            + Math.max(1, lockWindow.toMinutes()) + " minute(s).");
        }
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
        persistIssuedToken(user, access, "ACCESS");
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

    /** Persiste la traçabilité du token émis dans issued_tokens (MD-01 TokenJWT). */
    private void persistIssuedToken(AuthUserProfile user, String tokenRaw, String tokenType) {
        try {
            Claims claims = "ACCESS".equals(tokenType)
                    ? jwtService.parseAccessToken(tokenRaw)
                    : jwtService.parseRefreshToken(tokenRaw);
            String jti = claims.getId();
            Instant exp = jwtService.getExpiration(claims);
            issuedTokenRepository.save(new IssuedToken(jti, user.id(), user.username(), tokenType, exp));
        } catch (Exception ex) {
            // Non bloquant : la traçabilité est best-effort
        }
    }

    /** Enregistre une entrée dans le journal des connexions (MD-01 JournalConnexion). */
    private void journalLogin(String username, Long userId, LoginOutcome outcome, String ipAddress) {
        try {
            loginJournalRepository.save(new LoginJournalEntry(username, userId, outcome, ipAddress));
        } catch (Exception ex) {
            // Non bloquant : le journal est best-effort
        }
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
