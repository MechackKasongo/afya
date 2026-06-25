package com.afya.platform.audit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class IngestionKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String INGESTION_HEADER = "X-Audit-Ingestion-Key";

    private final String ingestionKey;

    public IngestionKeyAuthenticationFilter(@Value("${app.audit.ingestion-key:}") String ingestionKey) {
        this.ingestionKey = ingestionKey == null ? "" : ingestionKey.strip();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (ingestionKey.isEmpty() || !requiresIngestionKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            String header = request.getHeader(INGESTION_HEADER);
            if (ingestionKey.equals(header)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "system-ingestion",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresIngestionKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/v1/audit/events".equals(uri)) {
            return true;
        }
        return uri.startsWith("/api/v1/audit/internal/");
    }
}
