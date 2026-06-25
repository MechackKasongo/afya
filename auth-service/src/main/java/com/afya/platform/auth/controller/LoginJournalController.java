package com.afya.platform.auth.controller;

import com.afya.platform.auth.model.LoginJournalEntry;
import com.afya.platform.auth.model.LoginOutcome;
import com.afya.platform.auth.repository.LoginJournalRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Journal des connexions — MD-01 (JournalConnexion du mémoire).
 * Exposé sur {@code GET /api/v1/auth/login-journal} — accessible Admin uniquement.
 */
@RestController
@RequestMapping("/api/v1/auth/login-journal")
@PreAuthorize("hasRole('ADMIN')")
public class LoginJournalController {

    private final LoginJournalRepository loginJournalRepository;

    public LoginJournalController(LoginJournalRepository loginJournalRepository) {
        this.loginJournalRepository = loginJournalRepository;
    }

    /**
     * Liste paginée du journal de connexions.
     *
     * @param username  Filtre optionnel sur le nom d'utilisateur.
     * @param outcome   Filtre optionnel sur le résultat (SUCCESS, FAILURE_BAD_PASSWORD, …).
     * @param since     Filtre optionnel : n'afficher que les entrées après cette date (ISO-8601).
     * @param page      Numéro de page (0-indexed).
     * @param size      Taille de page (max 200).
     */
    @GetMapping
    public Page<LoginJournalEntry> getLoginJournal(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) LoginOutcome outcome,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());

        if (username != null && !username.isBlank()) {
            return loginJournalRepository.findByUsernameOrderByOccurredAtDesc(username.strip(), pageable);
        }
        if (outcome != null) {
            return loginJournalRepository.findByOutcomeOrderByOccurredAtDesc(outcome, pageable);
        }
        if (since != null) {
            return loginJournalRepository.findByOccurredAtAfterOrderByOccurredAtDesc(since, pageable);
        }
        return loginJournalRepository.findAll(pageable);
    }
}
