package com.afya.platform.auth.service;

import com.afya.platform.auth.dto.CreateCredentialRequest;
import com.afya.platform.auth.dto.SyncCredentialStatusRequest;
import com.afya.platform.auth.dto.UpdateCredentialPasswordRequest;
import com.afya.platform.auth.model.Credential;
import com.afya.platform.auth.model.CredentialStatus;
import com.afya.platform.auth.repository.CredentialRepository;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate requiresNewTransaction;
    private final int maxFailedAttempts;

    public CredentialService(
            CredentialRepository credentialRepository,
            PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager,
            @Value("${app.auth.max-failed-login-attempts:5}") int maxFailedAttempts
    ) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTransaction = template;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
    }

    public Optional<Credential> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return credentialRepository.findByUsernameIgnoreCase(username.strip());
    }

    @Transactional
    public void create(CreateCredentialRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        if (credentialRepository.existsByUserId(request.userId())) {
            throw new ConflictException("Un credential existe déjà pour l'utilisateur : " + request.userId());
        }
        if (credentialRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Identifiant déjà utilisé : " + normalizedUsername);
        }
        Credential credential = new Credential();
        credential.setUserId(request.userId());
        credential.setUsername(normalizedUsername);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setStatus(CredentialStatus.ACTIF);
        credential.setFailedAttempts(0);
        credentialRepository.save(credential);
    }

    @Transactional
    public void updatePassword(Long userId, UpdateCredentialPasswordRequest request) {
        Credential credential = findByUserId(userId);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        unlock(credential);
        credentialRepository.save(credential);
    }

    @Transactional
    public void syncStatus(Long userId, SyncCredentialStatusRequest request) {
        Credential credential = findByUserId(userId);
        if (Boolean.TRUE.equals(request.active())) {
            unlock(credential);
        } else {
            credential.setStatus(CredentialStatus.INACTIF);
        }
        credentialRepository.save(credential);
    }

    @Transactional
    public void delete(Long userId) {
        credentialRepository.findByUserId(userId).ifPresent(credentialRepository::delete);
    }

    @Transactional
    public boolean verifyPassword(Credential credential, String rawPassword) {
        if (credential.getStatus() == CredentialStatus.BLOQUE) {
            return false;
        }
        if (credential.getStatus() == CredentialStatus.INACTIF) {
            return false;
        }
        if (!passwordEncoder.matches(rawPassword, credential.getPasswordHash())) {
            recordFailedAttempt(credential);
            return false;
        }
        recordSuccessfulLogin(credential);
        return true;
    }

    public void recordFailedAttempt(Credential credential) {
        requiresNewTransaction.executeWithoutResult(status -> {
            Credential current = credentialRepository.findById(credential.getId())
                    .orElseThrow(() -> new NotFoundException("Credential introuvable : " + credential.getId()));
            int attempts = current.getFailedAttempts() + 1;
            current.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                current.setStatus(CredentialStatus.BLOQUE);
                current.setLockedAt(Instant.now());
            }
            credentialRepository.save(current);
        });
    }

    public void recordSuccessfulLogin(Credential credential) {
        requiresNewTransaction.executeWithoutResult(status -> {
            Credential current = credentialRepository.findById(credential.getId())
                    .orElseThrow(() -> new NotFoundException("Credential introuvable : " + credential.getId()));
            if (current.getStatus() == CredentialStatus.BLOQUE) {
                return;
            }
            current.setFailedAttempts(0);
            current.setLockedAt(null);
            current.setStatus(CredentialStatus.ACTIF);
            current.setLastAccessAt(Instant.now());
            credentialRepository.save(current);
        });
    }

    private Credential findByUserId(Long userId) {
        return credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Credential introuvable pour l'utilisateur : " + userId));
    }

    private static void unlock(Credential credential) {
        credential.setStatus(CredentialStatus.ACTIF);
        credential.setFailedAttempts(0);
        credential.setLockedAt(null);
    }

    private static String normalizeUsername(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }
}
