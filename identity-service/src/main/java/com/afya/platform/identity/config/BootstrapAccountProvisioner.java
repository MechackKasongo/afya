package com.afya.platform.identity.config;

import com.afya.platform.identity.model.AppUser;
import com.afya.platform.identity.model.Role;
import com.afya.platform.identity.repository.AppUserRepository;
import com.afya.platform.identity.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BootstrapAccountProvisioner implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean autoProvision;
    private final String username;
    private final String password;
    private final String fullName;

    public BootstrapAccountProvisioner(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.auto-provision:true}") boolean autoProvision,
            @Value("${app.bootstrap.username}") String username,
            @Value("${app.bootstrap.password}") String password,
            @Value("${app.bootstrap.full-name}") String fullName
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.autoProvision = autoProvision;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoProvision || appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rôle ADMIN introuvable — exécuter Flyway"));
        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setFullName(fullName);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRoles(new java.util.HashSet<>(java.util.Set.of(adminRole)));
        admin.setActive(true);
        appUserRepository.save(admin);
    }
}
