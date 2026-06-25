package com.afya.platform.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Table de traçabilité des tokens JWT émis — MD-01 (TokenJWT du mémoire).
 * Chaque access-token émis y est enregistré avec son JTI, sa date d'émission,
 * sa date d'expiration et la date de révocation éventuelle.
 *
 * <p>Différent de {@link RevokedAccessJti} qui stocke uniquement les JTIs révoqués
 * pour la validation rapide : cette table sert à l'audit (traçabilité complète).</p>
 */
@Entity
@Table(name = "issued_tokens")
public class IssuedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JTI (JWT ID) — identifiant unique du token. */
    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String username;

    /** Type de token : ACCESS ou REFRESH. */
    @Column(name = "token_type", nullable = false, length = 10)
    private String tokenType; // "ACCESS" | "REFRESH"

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Non-null si le token a été révoqué avant son expiration naturelle. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    public IssuedToken() {
    }

    public IssuedToken(String jti, Long userId, String username, String tokenType, Instant expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.username = username;
        this.tokenType = tokenType;
        this.issuedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    /** Marque le token comme révoqué à l'instant courant. */
    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    // ── Getters ──

    public Long getId() { return id; }
    public String getJti() { return jti; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTokenType() { return tokenType; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
