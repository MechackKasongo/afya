package com.afya.platform.identity.controller;

import com.afya.platform.identity.dto.LoginRequest;
import com.afya.platform.identity.dto.LogoutRequest;
import com.afya.platform.identity.dto.MeResponse;
import com.afya.platform.identity.dto.RefreshRequest;
import com.afya.platform.identity.dto.TokenResponse;
import com.afya.platform.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(
            Authentication authentication,
            HttpServletRequest request,
            @RequestBody(required = false) LogoutRequest body
    ) {
        boolean revokeAll = body == null || body.revokeAllSessions() == null || Boolean.TRUE.equals(body.revokeAllSessions());
        String refresh = body != null ? body.refreshToken() : null;
        authService.logout(authentication.getName(), request, refresh, revokeAll);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
