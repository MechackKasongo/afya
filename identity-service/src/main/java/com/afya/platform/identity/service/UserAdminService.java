package com.afya.platform.identity.service;

import com.afya.platform.identity.dto.*;
import com.afya.platform.identity.model.AppUser;
import com.afya.platform.identity.model.Role;
import com.afya.platform.identity.repository.AppUserRepository;
import com.afya.platform.identity.repository.RoleRepository;
import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialsLogService credentialsLogService;
    private final AuditEventPublisher auditEventPublisher;

    public UserAdminService(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            CredentialsLogService credentialsLogService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentialsLogService = credentialsLogService;
        this.auditEventPublisher = auditEventPublisher;
    }

    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    public Page<UserResponse> list(
            String query,
            String role,
            Boolean active,
            Long hospitalServiceId,
            Boolean withoutHospitalService,
            String sortBy,
            String sortDir,
            Integer page,
            Integer size
    ) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 20 : size;
        Sort sort = resolveSort(sortBy, sortDir);
        Specification<AppUser> spec = filterSpec(query, role, active, hospitalServiceId, withoutHospitalService);
        return appUserRepository.findAll(spec, PageRequest.of(safePage, safeSize, sort)).map(this::toResponse);
    }

    public List<RoleOptionResponse> listRoles() {
        return roleRepository.findAll(Sort.by("code")).stream()
                .map(r -> new RoleOptionResponse(r.getId(), "ROLE_" + r.getCode(), r.getLabel()))
                .toList();
    }

    public PasswordPreviewResponse previewPassword(PasswordPreviewRequest request) {
        int length = request.generatedPasswordLength() != null ? request.generatedPasswordLength() : 16;
        int variation = request.variation() != null ? request.variation() : 0;
        return new PasswordPreviewResponse(PasswordGenerator.suggest(
                request.firstName(),
                request.lastName(),
                request.postName(),
                length,
                variation));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        Role role = resolveRole(request.role());
        String fullName = resolveFullName(request);
        String username = resolveUsername(request, fullName);
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Nom d'utilisateur déjà utilisé : " + username);
        }
        String email = blankToNull(request.email());
        assertEmailAvailable(email, null);
        String plainPassword = resolvePassword(request);
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        assignRole(user, role);
        assignHospitalServiceIds(user, request.hospitalServiceIds());
        user.setActive(true);
        AppUser saved = appUserRepository.save(user);
        try {
            credentialsLogService.append(saved.getUsername(), plainPassword, saved.getFullName());
        } catch (RuntimeException ex) {
            log.warn(
                    "Compte {} créé mais journal des mots de passe non écrit : {}",
                    saved.getUsername(),
                    ex.getMessage());
        }
        auditEventPublisher.publish(
                "USER_CREATED",
                "USER",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("username", saved.getUsername()));
        return new UserResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                roleCodes(saved),
                saved.isActive(),
                List.copyOf(saved.getHospitalServiceIds()),
                plainPassword);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = findUser(id);
        user.setFullName(request.fullName().trim());
        String email = blankToNull(request.email());
        assertEmailAvailable(email, id);
        user.setEmail(email);
        assignRole(user, resolveRole(request.role()));
        if (request.hospitalServiceIds() != null) {
            assignHospitalServiceIds(user, request.hospitalServiceIds());
        }
        String newPlainPassword = null;
        if (request.password() != null && !request.password().isBlank()) {
            newPlainPassword = request.password().trim();
            user.setPasswordHash(passwordEncoder.encode(newPlainPassword));
        }
        AppUser saved = appUserRepository.save(user);
        if (newPlainPassword != null) {
            try {
                credentialsLogService.append(saved.getUsername(), newPlainPassword, saved.getFullName());
            } catch (RuntimeException ex) {
                log.warn("Mot de passe mis à jour mais journal non écrit : {}", ex.getMessage());
            }
        }
        auditEventPublisher.publish(
                "USER_UPDATED",
                "USER",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                null);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateStatus(Long id, boolean active) {
        AppUser user = findUser(id);
        user.setActive(active);
        AppUser saved = appUserRepository.save(user);
        auditEventPublisher.publish(
                active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "USER",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                null);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = findUser(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BadRequestException("Le compte administrateur bootstrap ne peut pas être supprimé");
        }
        appUserRepository.delete(user);
        auditEventPublisher.publish(
                "USER_DELETED",
                "USER",
                AuditMetadata.resourceId(id),
                AuditActorResolver.currentUsername(),
                null);
    }

    public UserCredentialsResponse credentialsForUser(Long id) {
        AppUser user = findUser(id);
        return credentialsLogService
                .findLatestForUsername(user.getUsername())
                .map(line -> new UserCredentialsResponse(
                        user.getUsername(), line.password(), true, line.loggedAt()))
                .orElse(new UserCredentialsResponse(user.getUsername(), null, false, null));
    }

    public CredentialsLogPreviewResponse credentialsPreview() {
        return credentialsLogService.preview();
    }

    public byte[] credentialsFile() {
        return credentialsLogService.readAllBytes();
    }

    public void deleteCredentialsFile() {
        credentialsLogService.delete();
    }

    private AppUser findUser(Long id) {
        return appUserRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.withoutPassword(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleCodes(user),
                user.isActive(),
                List.copyOf(user.getHospitalServiceIds()));
    }

    private static List<String> roleCodes(AppUser user) {
        return user.getRoles().stream().map(Role::getCode).sorted().map(UserAdminService::toUiRole).toList();
    }

    static String toUiRole(String code) {
        return "ROLE_" + code;
    }

    static String fromUiRole(String role) {
        String trimmed = role == null ? "" : role.trim();
        if (trimmed.startsWith("ROLE_")) {
            return trimmed.substring(5);
        }
        return trimmed;
    }

    private Role resolveRole(String roleInput) {
        String code = fromUiRole(roleInput);
        return roleRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Rôle inconnu : " + roleInput));
    }

    private static String resolveFullName(UserCreateRequest request) {
        if (request.fullName() != null && !request.fullName().isBlank()) {
            return request.fullName().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (request.firstName() != null) {
            sb.append(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(request.lastName().trim());
        }
        if (request.postName() != null && !request.postName().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(request.postName().trim());
        }
        if (sb.isEmpty()) {
            throw new BadRequestException("Le nom complet est obligatoire");
        }
        return sb.toString();
    }

    private String resolveUsername(UserCreateRequest request, String fullName) {
        if (request.username() != null && !request.username().isBlank()) {
            return request.username().trim().toLowerCase(Locale.ROOT);
        }
        String[] parts = fullName.toLowerCase(Locale.ROOT).split("\\s+");
        String base = parts.length >= 2 ? parts[0] + parts[parts.length - 1] : parts[0];
        base = base.replaceAll("[^a-z0-9]", "");
        String candidate = base;
        int suffix = 1;
        while (appUserRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String resolvePassword(UserCreateRequest request) {
        if (request.password() != null && !request.password().isBlank()) {
            return request.password().trim();
        }
        int length = request.generatedPasswordLength() != null ? request.generatedPasswordLength() : 16;
        int variation = request.passwordVariation() != null ? request.passwordVariation() : 0;
        return PasswordGenerator.suggest(
                request.firstName() != null ? request.firstName() : "",
                request.lastName() != null ? request.lastName() : "",
                request.postName(),
                length,
                variation);
    }

    private static List<Long> normalizeServiceIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Hibernate doit pouvoir modifier la collection (pas de Set.of immuable). */
    private static void assignRole(AppUser user, Role role) {
        Set<Role> roles = user.getRoles();
        if (roles == null) {
            user.setRoles(new HashSet<>(Set.of(role)));
            return;
        }
        roles.clear();
        roles.add(role);
    }

    /** Même contrainte que pour les rôles : modifier la collection en place. */
    private static void assignHospitalServiceIds(AppUser user, List<Long> ids) {
        Set<Long> services = user.getHospitalServiceIds();
        List<Long> normalized = normalizeServiceIds(ids);
        if (services == null) {
            user.setHospitalServiceIds(new HashSet<>(normalized));
            return;
        }
        services.clear();
        services.addAll(normalized);
    }

    private void assertEmailAvailable(String email, Long excludeUserId) {
        if (email == null) {
            return;
        }
        boolean taken = excludeUserId == null
                ? appUserRepository.existsByEmailIgnoreCase(email)
                : appUserRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeUserId);
        if (taken) {
            throw new ConflictException("Cette adresse email est déjà utilisée par un autre compte.");
        }
    }

    private static Specification<AppUser> filterSpec(
            String query,
            String role,
            Boolean active,
            Long hospitalServiceId,
            Boolean withoutHospitalService
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (role != null && !role.isBlank()) {
                String code = fromUiRole(role);
                Join<AppUser, Role> rolesJoin = root.join("roles");
                predicates.add(cb.equal(cb.lower(rolesJoin.get("code")), code.toLowerCase(Locale.ROOT)));
                cq.distinct(true);
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (Boolean.TRUE.equals(withoutHospitalService)) {
                predicates.add(cb.isEmpty(root.get("hospitalServiceIds")));
            } else if (hospitalServiceId != null) {
                predicates.add(cb.isMember(hospitalServiceId, root.get("hospitalServiceIds")));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Sort resolveSort(String sortBy, String sortDir) {
        Sort.Direction dir = resolveSortDirection(sortDir);
        if ("role".equalsIgnoreCase(sortBy)) {
            return JpaSort.unsafe(dir, "roles.code");
        }
        return Sort.by(dir, resolveSortProperty(sortBy));
    }

    private static Sort.Direction resolveSortDirection(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private static String resolveSortProperty(String sortBy) {
        if (sortBy == null) {
            return "id";
        }
        return switch (sortBy) {
            case "username" -> "username";
            case "fullName" -> "fullName";
            case "active" -> "active";
            default -> "id";
        };
    }
}
