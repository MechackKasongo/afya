package com.afya.platform.lab.config;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lab/exam-types").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/lab/exam-types").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/lab/exam-requests")
                        .hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/lab/exam-requests/**")
                        .hasAnyRole("ADMIN", "MEDECIN", "INFIRMIER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/lab/exam-requests/*/specimen")
                        .hasAnyRole("ADMIN", "MEDECIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/lab/exam-requests/*/result")
                        .hasAnyRole("ADMIN", "MEDECIN")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
