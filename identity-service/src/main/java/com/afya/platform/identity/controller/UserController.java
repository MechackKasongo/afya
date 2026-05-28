package com.afya.platform.identity.controller;

import com.afya.platform.identity.dto.*;
import com.afya.platform.identity.service.UserAdminService;
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
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
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
            @RequestParam(required = false) Integer size
    ) {
        return userAdminService.list(
                query, role, active, hospitalServiceId, withoutHospitalService, sortBy, sortDir, page, size);
    }

    @GetMapping("/roles")
    public List<RoleOptionResponse> listRoles() {
        return userAdminService.listRoles();
    }

    @PostMapping("/password-preview")
    public PasswordPreviewResponse passwordPreview(@Valid @RequestBody PasswordPreviewRequest request) {
        return userAdminService.previewPassword(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userAdminService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return userAdminService.updateStatus(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userAdminService.delete(id);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userAdminService.getById(id);
    }

    @GetMapping("/{id}/credentials")
    public UserCredentialsResponse credentialsForUser(@PathVariable Long id) {
        return userAdminService.credentialsForUser(id);
    }

    @GetMapping("/credentials-log/preview")
    public CredentialsLogPreviewResponse credentialsPreview() {
        return userAdminService.credentialsPreview();
    }

    @GetMapping(value = "/credentials-log", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> credentialsFile() {
        byte[] body = userAdminService.credentialsFile();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.txt\"")
                .body(body);
    }

    @GetMapping(value = "/credentials-log.csv", produces = "text/csv")
    public ResponseEntity<byte[]> credentialsCsv() {
        byte[] raw = userAdminService.credentialsFile();
        String csv = "timestamp;username;fullName;password\n"
                + new String(raw).lines()
                        .map(line -> {
                            String[] parts = line.split("\\|");
                            if (parts.length < 4) {
                                return line;
                            }
                            return String.join(
                                    ";",
                                    parts[0].trim(),
                                    parts[1].trim(),
                                    parts[2].trim(),
                                    parts[3].trim());
                        })
                        .collect(java.util.stream.Collectors.joining("\n"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.csv\"")
                .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @DeleteMapping("/credentials-log")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCredentialsLog() {
        userAdminService.deleteCredentialsFile();
    }
}
