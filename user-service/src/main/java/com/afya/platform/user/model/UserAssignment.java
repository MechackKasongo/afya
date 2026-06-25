package com.afya.platform.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Affectation d'un utilisateur à un service hospitalier — MD-02 (Affectation du mémoire).
 *
 * <p>Remplace le simple {@code Set<Long> hospitalServiceIds} de {@link AppUser}
 * par une entité datée permettant de tracer les périodes d'affectation
 * (début, fin, service hospitalier).</p>
 *
 * <p>Un utilisateur peut avoir plusieurs affectations simultanées (multi-service)
 * ou successives dans le temps.</p>
 */
@Entity
@Table(name = "user_assignments")
public class UserAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur affecté. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Identifiant du service hospitalier (référence vers hospital-service). */
    @Column(name = "hospital_service_id", nullable = false)
    private Long hospitalServiceId;

    /** Date de début d'affectation (incluse). */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Date de fin d'affectation (incluse).
     * {@code null} signifie que l'affectation est toujours active.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    public UserAssignment() {
    }

    public UserAssignment(Long userId, Long hospitalServiceId, LocalDate startDate) {
        this.userId = userId;
        this.hospitalServiceId = hospitalServiceId;
        this.startDate = startDate;
    }

    /** @return {@code true} si l'affectation est en cours (endDate null ou future). */
    public boolean isActive() {
        return endDate == null || !endDate.isBefore(LocalDate.now());
    }

    // ── Getters / Setters ──

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getHospitalServiceId() { return hospitalServiceId; }
    public void setHospitalServiceId(Long hospitalServiceId) { this.hospitalServiceId = hospitalServiceId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
