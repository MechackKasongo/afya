package com.afya.platform.auth.repository;

import com.afya.platform.auth.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByUsernameIgnoreCase(String username);

    Optional<Credential> findByUserId(Long userId);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUserId(Long userId);
}
