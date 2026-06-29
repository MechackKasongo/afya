package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.UserClient;
import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserBffController {

    private final UserClient userClient;

    public UserBffController(UserClient userClient) {
        this.userClient = userClient;
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
        return userClient.listUsers(
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
        return userClient.listRoles(AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/password-preview")
    public PasswordPreviewResponse passwordPreview(
            @Valid @RequestBody PasswordPreviewRequest body,
            HttpServletRequest request
    ) {
        return userClient.passwordPreview(
                body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest body, HttpServletRequest request) {
        return userClient.createUser(
                body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest body,
            HttpServletRequest request
    ) {
        return userClient.updateUser(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest body,
            HttpServletRequest request
    ) {
        return userClient.updateUserStatus(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest request) {
        userClient.deleteUser(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id, HttpServletRequest request) {
        return userClient.getUser(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
