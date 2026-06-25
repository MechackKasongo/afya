package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.AuthClient;
import com.afya.platform.bff.dto.LoginRequest;
import com.afya.platform.bff.dto.MeResponse;
import com.afya.platform.bff.dto.RefreshRequest;
import com.afya.platform.bff.dto.TokenResponse;
import com.afya.platform.bff.service.MeEnrichmentService;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthBffController {

    private final AuthClient authClient;
    private final MeEnrichmentService meEnrichmentService;

    public AuthBffController(AuthClient authClient, MeEnrichmentService meEnrichmentService) {
        this.authClient = authClient;
        this.meEnrichmentService = meEnrichmentService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authClient.login(request);
        return meEnrichmentService.enrichToken(tokens, "Bearer " + tokens.accessToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse tokens = authClient.refresh(request);
        return meEnrichmentService.enrichToken(tokens, "Bearer " + tokens.accessToken());
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        String bearer = AuthorizationSupport.requireBearer(request.getHeader("Authorization"));
        MeResponse me = authClient.me(bearer);
        return meEnrichmentService.enrich(me, bearer);
    }

    @PostMapping("/logout")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authClient.logout(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
