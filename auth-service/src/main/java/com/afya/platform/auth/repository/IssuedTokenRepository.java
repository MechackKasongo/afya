package com.afya.platform.auth.repository;

import com.afya.platform.auth.model.IssuedToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Accès aux tokens émis — MD-01 (TokenJWT).
 */
@Repository
public interface IssuedTokenRepository extends JpaRepository<IssuedToken, Long> {

    Optional<IssuedToken> findByJti(String jti);

    Page<IssuedToken> findByUserIdOrderByIssuedAtDesc(Long userId, Pageable pageable);

    Page<IssuedToken> findByUsernameOrderByIssuedAtDesc(String username, Pageable pageable);

    /** Supprime les tokens expirés antérieurs à la date indiquée (nettoyage planifié). */
    @Modifying
    @Query("DELETE FROM IssuedToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);

    /** Compte les tokens actifs (non révoqués, non expirés) d'un utilisateur. */
    @Query("SELECT COUNT(t) FROM IssuedToken t WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now")
    long countActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
