package com.afya.platform.auth.config;

import com.afya.platform.auth.repository.RevokedAccessJtiRepository;
import com.afya.platform.auth.service.JwtService;
import com.afya.platform.shared.security.JwtAuthDetails;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RevokedAccessJtiRepository revokedAccessJtiRepository;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedAccessJtiRepository revokedAccessJtiRepository) {
        this.jwtService = jwtService;
        this.revokedAccessJtiRepository = revokedAccessJtiRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parseAccessToken(token);
                if (!revokedAccessJtiRepository.existsByJti(claims.getId())) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = claims.get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles == null
                            ? List.of()
                            : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
                    @SuppressWarnings("unchecked")
                    List<Number> rawServiceIds = claims.get("hospitalServiceIds", List.class);
                    List<Long> hospitalServiceIds = rawServiceIds == null
                            ? List.of()
                            : rawServiceIds.stream().map(Number::longValue).toList();
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, authorities);
                    auth.setDetails(new JwtAuthDetails(roles, hospitalServiceIds));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
