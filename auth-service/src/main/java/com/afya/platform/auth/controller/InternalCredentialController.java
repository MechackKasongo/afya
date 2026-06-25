package com.afya.platform.auth.controller;

import com.afya.platform.auth.dto.CreateCredentialRequest;
import com.afya.platform.auth.dto.SyncCredentialStatusRequest;
import com.afya.platform.auth.dto.UpdateCredentialPasswordRequest;
import com.afya.platform.auth.service.CredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/credentials")
public class InternalCredentialController {

    private final CredentialService credentialService;

    public InternalCredentialController(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateCredentialRequest request) {
        credentialService.create(request);
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateCredentialPasswordRequest request
    ) {
        credentialService.updatePassword(userId, request);
    }

    @PatchMapping("/{userId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncStatus(
            @PathVariable Long userId,
            @Valid @RequestBody SyncCredentialStatusRequest request
    ) {
        credentialService.syncStatus(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        credentialService.delete(userId);
    }
}
