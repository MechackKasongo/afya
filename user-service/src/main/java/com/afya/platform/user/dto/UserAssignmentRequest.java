package com.afya.platform.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requête de création d'une affectation datée — MD-02 (Affectation).
 *
 * @param hospitalServiceId ID du service hospitalier cible (obligatoire).
 * @param startDate         Date de début (obligatoire).
 * @param endDate           Date de fin (optionnelle — null = affectation
 *                          ouverte).
 */
public record UserAssignmentRequest(

                @NotNull(message = "L'identifiant du service hospitalier est obligatoire") Long hospitalServiceId,

                @NotNull(message = "La date de début est obligatoire") LocalDate startDate,

                LocalDate endDate) {
}
