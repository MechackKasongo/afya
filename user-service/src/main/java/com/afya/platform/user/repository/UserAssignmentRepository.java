package com.afya.platform.user.repository;

import com.afya.platform.user.model.UserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Accès aux affectations utilisateur-service — MD-02 (Affectation du mémoire).
 */
@Repository
public interface UserAssignmentRepository extends JpaRepository<UserAssignment, Long> {

    /** Toutes les affectations d'un utilisateur (historique complet). */
    List<UserAssignment> findByUserIdOrderByStartDateDesc(Long userId);

    /** Affectations actives d'un utilisateur à une date donnée. */
    @Query("""
            SELECT a FROM UserAssignment a
            WHERE a.userId = :userId
              AND a.startDate <= :date
              AND (a.endDate IS NULL OR a.endDate >= :date)
            """)
    List<UserAssignment> findActiveForUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** Affectations actives d'un utilisateur (endDate null ou future). */
    @Query("""
            SELECT a FROM UserAssignment a
            WHERE a.userId = :userId
              AND (a.endDate IS NULL OR a.endDate >= :today)
            ORDER BY a.startDate DESC
            """)
    List<UserAssignment> findCurrentForUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** Tous les utilisateurs actifs dans un service hospitalier donné. */
    @Query("""
            SELECT a FROM UserAssignment a
            WHERE a.hospitalServiceId = :serviceId
              AND (a.endDate IS NULL OR a.endDate >= :today)
            """)
    List<UserAssignment> findActiveByHospitalService(
            @Param("serviceId") Long serviceId,
            @Param("today") LocalDate today);

    void deleteByUserId(Long userId);
}
