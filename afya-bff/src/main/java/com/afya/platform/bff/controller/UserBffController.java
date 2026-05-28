package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.IdentityClient;
import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserBffController {

    private final IdentityClient identityClient;

    public UserBffController(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @GetMapping
    public Page<UserResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long hospitalServiceId,
            @RequestParam(required = false) Boolean withoutHospitalService,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return identityClient.listUsers(
                query,
                role,
                active,
                hospitalServiceId,
                withoutHospitalService,
                sortBy,
                sortDir,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/roles")
    public List<RoleOptionResponse> listRoles(HttpServletRequest request) {
        return identityClient.listRoles(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/password-preview")
    public PasswordPreviewResponse passwordPreview(
            @Valid @RequestBody PasswordPreviewRequest body,
            HttpServletRequest request
    ) {
        return identityClient.passwordPreview(
                body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest body, HttpServletRequest request) {
        return identityClient.createUser(
                body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest body,
            HttpServletRequest request
    ) {
        return identityClient.updateUser(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest body,
            HttpServletRequest request
    ) {
        return identityClient.updateUserStatus(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        identityClient.deleteUser(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id, HttpServletRequest request) {
        return identityClient.getUser(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/credentials")
    public UserCredentialsResponse credentialsForUser(@PathVariable Long id, HttpServletRequest request) {
        return identityClient.credentialsForUser(
                id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/credentials-log/preview")
    public CredentialsLogPreviewResponse credentialsPreview(HttpServletRequest request) {
        return identityClient.credentialsPreview(
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping(value = "/credentials-log", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> credentialsFile(HttpServletRequest request) {
        byte[] body = identityClient.credentialsFile(
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.txt\"")
                .body(body);
    }

    @GetMapping(value = "/credentials-log.csv", produces = "text/csv")
    public ResponseEntity<byte[]> credentialsCsv(HttpServletRequest request) {
        byte[] body = identityClient.credentialsCsv(
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.csv\"")
                .body(body);
    }

    @DeleteMapping("/credentials-log")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCredentialsLog(HttpServletRequest request) {
        identityClient.deleteCredentialsLog(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
