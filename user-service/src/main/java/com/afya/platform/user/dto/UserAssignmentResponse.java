package com.afya.platform.user.dto;

import java.time.LocalDate;

/**
 * Réponse DTO pour une affectation utilisateur-service hospitalier — MD-02 (Affectation).
 *
 * @param id                ID de l'affectation.
 * @param userId            ID de l'utilisateur.
 * @param hospitalServiceId ID du service hospitalier.
 * @param startDate         Date de début d'affectation.
 * @param endDate           Date de fin d'affectation (null = active).
 * @param active            Vrai si l'affectation est en cours.
 */
public record UserAssignmentResponse(
        Long id,
        Long userId,
        Long hospitalServiceId,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {
}
