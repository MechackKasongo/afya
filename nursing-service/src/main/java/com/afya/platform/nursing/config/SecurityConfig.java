package com.afya.platform.nursing.config;

import com.afya.platform.shared.security.JwtAccessTokenValidator;
import com.afya.platform.shared.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtAccessTokenValidator validator) {
        return new JwtAuthenticationFilter(validator);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            InternalServiceKeyAuthenticationFilter internalKeyFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole("SYSTEM")
                        .requestMatchers(HttpMethod.GET, "/api/v1/patients/*/nursing-care",
                                "/api/v1/patients/*/prescription-notifications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admissions/*/vital-signs",
                                "/api/v1/admissions/*/vital-sign-alerts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/patients/*/prescription-notifications/*/read")
                        .hasAnyRole("ADMIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/patients/*/nursing-care",
                                "/api/v1/prescriptions/*/administrations",
                                "/api/v1/admissions/*/vital-signs")
                        .hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(internalKeyFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
