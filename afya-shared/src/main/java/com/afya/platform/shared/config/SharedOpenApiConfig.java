package com.afya.platform.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI/Swagger partagée par tous les services Afya.
 *
 * <p>Fournit, de façon homogène :
 * <ul>
 *   <li>un titre dérivé du nom applicatif ({@code spring.application.name}) ;</li>
 *   <li>un schéma de sécurité « bearer JWT » et le bouton <em>Authorize</em> de Swagger UI,
 *       afin de pouvoir tester les endpoints protégés avec un access token.</li>
 * </ul>
 *
 * <p>Placée dans {@code com.afya.platform.shared}, elle est récupérée par le
 * {@code scanBasePackages} commun à tous les modules. En production, Swagger est désactivé
 * via {@code springdoc.*.enabled=false} (voir {@code application-prod.properties}).
 */
@Configuration
public class SharedOpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI afyaOpenApi(
            @Value("${spring.application.name:afya-service}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Afya — " + applicationName)
                        .version("v1")
                        .description("API REST du module « " + applicationName
                                + " » de la plateforme hospitalière Afya."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Coller l'access token JWT (sans le préfixe « Bearer »).")));
    }
}
