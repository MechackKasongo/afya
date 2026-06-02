package com.afya.platform.shared.security;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * Fails fast on invalid downstream base URLs.
 */
@Component
public class DownstreamUrlsValidator implements ApplicationRunner {

    private static final List<String> URL_KEYS = List.of(
            "app.services.identity-base-url",
            "app.services.catalog-base-url",
            "app.services.patient-base-url",
            "app.services.care-entry-base-url",
            "app.services.stay-base-url",
            "app.services.clinical-base-url",
            "app.services.audit-base-url"
    );

    private final Environment environment;

    public DownstreamUrlsValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String key : URL_KEYS) {
            String value = environment.getProperty(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            validateUrl(key, value);
        }
    }

    private void validateUrl(String key, String rawValue) {
        try {
            URI uri = URI.create(rawValue.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw invalid(key, rawValue, "schéma invalide (attendu: http ou https)");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw invalid(key, rawValue, "hôte manquant");
            }
        } catch (IllegalArgumentException ex) {
            throw invalid(key, rawValue, "format URI invalide");
        }
    }

    private IllegalStateException invalid(String key, String value, String reason) {
        return new IllegalStateException(
                "Configuration invalide: '" + key + "' = '" + value + "' (" + reason + ")"
        );
    }
}
