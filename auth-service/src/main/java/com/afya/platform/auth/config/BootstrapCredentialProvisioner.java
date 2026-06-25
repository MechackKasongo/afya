package com.afya.platform.auth.config;

import com.afya.platform.auth.dto.CreateCredentialRequest;
import com.afya.platform.auth.integration.AuthUserProfile;
import com.afya.platform.auth.integration.UserServiceClient;
import com.afya.platform.auth.service.CredentialService;
import com.afya.platform.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapCredentialProvisioner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapCredentialProvisioner.class);

    private final CredentialService credentialService;
    private final UserServiceClient userServiceClient;
    private final boolean autoProvision;
    private final String username;
    private final String password;

    public BootstrapCredentialProvisioner(
            CredentialService credentialService,
            UserServiceClient userServiceClient,
            @Value("${app.bootstrap.auto-provision:true}") boolean autoProvision,
            @Value("${app.bootstrap.username:admin}") String username,
            @Value("${app.bootstrap.password:}") String password
    ) {
        this.credentialService = credentialService;
        this.userServiceClient = userServiceClient;
        this.autoProvision = autoProvision;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoProvision || password == null || password.isBlank()) {
            return;
        }
        if (credentialService.findByUsername(username).isPresent()) {
            return;
        }
        try {
            AuthUserProfile profile = userServiceClient.findByUsername(username);
            credentialService.create(new CreateCredentialRequest(profile.id(), profile.username(), password));
            log.info("Credential bootstrap créé pour {}", profile.username());
        } catch (NotFoundException ex) {
            log.debug("Utilisateur bootstrap {} absent dans user-service — credential non créé", username);
        } catch (RuntimeException ex) {
            log.warn("Credential bootstrap non créé pour {} : {}", username, ex.getMessage());
        }
    }
}
