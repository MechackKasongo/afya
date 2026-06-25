package com.afya.platform.user.config;

import com.afya.platform.user.model.AppUser;
import com.afya.platform.user.model.Role;
import com.afya.platform.user.repository.AppUserRepository;
import com.afya.platform.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAccountProvisioner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAccountProvisioner.class);

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final boolean autoProvision;
    private final String username;
    private final String fullName;

    public BootstrapAccountProvisioner(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            @Value("${app.bootstrap.auto-provision:true}") boolean autoProvision,
            @Value("${app.bootstrap.username}") String username,
            @Value("${app.bootstrap.full-name}") String fullName
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.autoProvision = autoProvision;
        this.username = username;
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
        admin.setRoles(new java.util.HashSet<>(java.util.Set.of(adminRole)));
        admin.setActive(true);
        appUserRepository.save(admin);
        log.info("Compte bootstrap {} créé dans user-service (credential provisionné par auth-service)", username);
    }
}
