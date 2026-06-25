package com.afya.platform.auth.service;

import com.afya.platform.auth.repository.IssuedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nettoyage planifié des tokens expirés dans {@code issued_tokens}.
 *
 * <p>Supprime chaque nuit les entrées expirées depuis plus de 7 jours
 * (conservation d'un historique court pour l'audit).
 * Ce scheduler empêche la table {@code issued_tokens} de grossir indéfiniment.</p>
 */
@Service
public class IssuedTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IssuedTokenCleanupScheduler.class);

    /** Durée de rétention des tokens expirés avant suppression physique (7 jours). */
    private static final long RETENTION_DAYS = 7;

    private final IssuedTokenRepository issuedTokenRepository;

    public IssuedTokenCleanupScheduler(IssuedTokenRepository issuedTokenRepository) {
        this.issuedTokenRepository = issuedTokenRepository;
    }

    /**
     * Supprime les tokens expirés depuis plus de {@value RETENTION_DAYS} jours.
     * Exécution quotidienne à 03:00 UTC.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = issuedTokenRepository.deleteExpiredBefore(cutoff);
        log.info("[IssuedTokenCleanup] {} token(s) expiré(s) supprimé(s) (antérieurs à {})", deleted, cutoff);
    }
}
