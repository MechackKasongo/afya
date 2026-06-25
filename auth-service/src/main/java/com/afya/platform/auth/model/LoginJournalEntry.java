package com.afya.platform.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Journal des connexions — MD-01 (JournalConnexion du mémoire).
 *
 * <p>Enregistre chaque tentative de connexion (réussie ou échouée) avec :
 * <ul>
 *   <li>L'identifiant de l'utilisateur concerné</li>
 *   <li>Le résultat de la tentative ({@link LoginOutcome})</li>
 *   <li>L'adresse IP cliente</li>
 *   <li>L'horodatage</li>
 * </ul>
 */
@Entity
@Table(name = "login_journal")
public class LoginJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username utilisé lors de la tentative (peut ne pas correspondre à un compte existant). */
    @Column(nullable = false, length = 80)
    private String username;

    /** Identifiant de l'utilisateur si le compte a été trouvé, null sinon. */
    @Column(name = "user_id")
    private Long userId;

    /** Résultat de la tentative de connexion. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LoginOutcome outcome;

    /** Adresse IP de la requête (peut être null si non transmise). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Horodatage de la tentative. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public LoginJournalEntry() {
    }

    public LoginJournalEntry(String username, Long userId, LoginOutcome outcome, String ipAddress) {
        this.username = username;
        this.userId = userId;
        this.outcome = outcome;
        this.ipAddress = ipAddress;
        this.occurredAt = Instant.now();
    }

    // ── Getters ──

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LoginOutcome getOutcome() { return outcome; }
    public void setOutcome(LoginOutcome outcome) { this.outcome = outcome; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getOccurredAt() { return occurredAt; }
}
