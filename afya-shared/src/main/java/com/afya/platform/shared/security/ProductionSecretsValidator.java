package com.afya.platform.shared.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * Prevents accidental startup in non-dev profiles with unsafe defaults.
 */
@Component
public class ProductionSecretsValidator implements ApplicationRunner {

    private static final Set<String> NON_PROD_PROFILES = Set.of("dev", "test", "docker", "local");

    private final Environment environment;

    public ProductionSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionLikeProfile()) {
            return;
        }
        assertNotDefault(
                "app.jwt.access-secret",
                "dev-access-secret-at-least-64-characters-long-for-hs512-signing-key"
        );
        assertNotDefault("app.audit.ingestion-key", "dev-audit-ingestion-key");
        assertNotDefault("app.bootstrap.password", "Admin@Afya2026!");

        String autoProvision = environment.getProperty("app.bootstrap.auto-provision");
        if ("true".equalsIgnoreCase(autoProvision)) {
            throw new IllegalStateException(
                    "Sécurité: app.bootstrap.auto-provision doit être désactivé hors dev/test."
            );
        }
    }

    private boolean isProductionLikeProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        return Arrays.stream(activeProfiles)
                .map(String::toLowerCase)
                .noneMatch(NON_PROD_PROFILES::contains);
    }

    private void assertNotDefault(String key, String unsafeValue) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            return;
        }
        if (unsafeValue.equals(value)) {
            throw new IllegalStateException(
                    "Sécurité: propriété '" + key + "' utilise une valeur par défaut interdite en production."
            );
        }
    }
}
