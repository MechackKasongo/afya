package com.afya.platform.user.controller;

import com.afya.platform.user.dto.UserAssignmentRequest;
import com.afya.platform.user.dto.UserAssignmentResponse;
import com.afya.platform.user.model.UserAssignment;
import com.afya.platform.user.repository.AppUserRepository;
import com.afya.platform.user.repository.UserAssignmentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestion des affectations datées utilisateur → service hospitalier — MD-02.
 *
 * <p>Endpoints :
 * <ul>
 *   <li>{@code GET  /api/v1/users/{userId}/assignments} — liste complète</li>
 *   <li>{@code GET  /api/v1/users/{userId}/assignments/current} — affectations actives</li>
 *   <li>{@code POST /api/v1/users/{userId}/assignments} — nouvelle affectation</li>
 *   <li>{@code DELETE /api/v1/users/{userId}/assignments/{id}} — clôturer</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class UserAssignmentController {

    private final UserAssignmentRepository assignmentRepository;
    private final AppUserRepository userRepository;

    public UserAssignmentController(
            UserAssignmentRepository assignmentRepository,
            AppUserRepository userRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    /** Historique complet des affectations d'un utilisateur. */
    @GetMapping
    public List<UserAssignmentResponse> list(@PathVariable Long userId) {
        requireUserExists(userId);
        return assignmentRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Affectations actuellement actives (endDate null ou future). */
    @GetMapping("/current")
    public List<UserAssignmentResponse> current(@PathVariable Long userId) {
        requireUserExists(userId);
        return assignmentRepository.findCurrentForUser(userId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Crée une nouvelle affectation datée. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAssignmentResponse create(
            @PathVariable Long userId,
            @Valid @RequestBody UserAssignmentRequest request
    ) {
        requireUserExists(userId);
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être postérieure ou égale à la date de début");
        }
        UserAssignment assignment = new UserAssignment(
                userId,
                request.hospitalServiceId(),
                request.startDate()
        );
        assignment.setEndDate(request.endDate());
        return toResponse(assignmentRepository.save(assignment));
    }

    /** Clôture une affectation en fixant la date de fin à aujourd'hui. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable Long userId, @PathVariable Long id) {
        requireUserExists(userId);
        UserAssignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation introuvable"));
        if (assignment.getEndDate() == null || assignment.getEndDate().isAfter(LocalDate.now())) {
            assignment.setEndDate(LocalDate.now());
            assignmentRepository.save(assignment);
        }
    }

    private void requireUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable : " + userId);
        }
    }

    private UserAssignmentResponse toResponse(UserAssignment a) {
        return new UserAssignmentResponse(
                a.getId(),
                a.getUserId(),
                a.getHospitalServiceId(),
                a.getStartDate(),
                a.getEndDate(),
                a.isActive()
        );
    }
}
