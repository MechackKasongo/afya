package com.afya.platform.auth.repository;

import com.afya.platform.auth.model.LoginJournalEntry;
import com.afya.platform.auth.model.LoginOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;

/**
 * Accès au journal des connexions — MD-01 (JournalConnexion).
 */
@Repository
public interface LoginJournalRepository extends JpaRepository<LoginJournalEntry, Long> {

    Page<LoginJournalEntry> findByUsernameOrderByOccurredAtDesc(String username, Pageable pageable);

    Page<LoginJournalEntry> findByOutcomeOrderByOccurredAtDesc(LoginOutcome outcome, Pageable pageable);

    Page<LoginJournalEntry> findByOccurredAtAfterOrderByOccurredAtDesc(Instant since, Pageable pageable);

    long countByUsernameAndOutcomeAndOccurredAtAfter(String username, LoginOutcome outcome, Instant since);

    /** Compte les tentatives infructueuses récentes pour un username (protection anti-force brute). */
    long countByUsernameIgnoreCaseAndOutcomeInAndOccurredAtAfter(
            String username, Collection<LoginOutcome> outcomes, Instant since);
}
