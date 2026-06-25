package com.afya.platform.report.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Statistiques pré-agrégées des admissions — MD-09 (StatistiqueAdmission du mémoire).
 * Alimentée quotidiennement par {@code AdmissionStatsScheduler}.
 *
 * <p>Stocke les indicateurs clés pour les tableaux de bord sans requery temps-réel :</p>
 * <ul>
 *   <li>Admissions du jour</li>
 *   <li>Durée moyenne de séjour (jours)</li>
 *   <li>Taux d'occupation des lits (en %)</li>
 *   <li>Décès, transferts, sorties</li>
 * </ul>
 */
@Entity
@Table(name = "admission_stats")
public class AdmissionStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Date de référence des statistiques (jour J). */
    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    /** Nombre d'admissions créées ce jour. */
    @Column(name = "admissions_count", nullable = false)
    private int admissionsCount;

    /** Nombre de sorties (statut SORTI) ce jour. */
    @Column(name = "discharges_count", nullable = false)
    private int dischargesCount;

    /** Nombre de transferts ce jour. */
    @Column(name = "transfers_count", nullable = false)
    private int transfersCount;

    /** Nombre de décès déclarés ce jour. */
    @Column(name = "deaths_count", nullable = false)
    private int deathsCount;

    /** Admissions actives (EN_COURS) au moment du calcul. */
    @Column(name = "active_admissions", nullable = false)
    private int activeAdmissions;

    /** Durée moyenne des séjours clos calculée sur les 30 derniers jours (en jours). */
    @Column(name = "avg_stay_days")
    private Double avgStayDays;

    /** Taux d'occupation global des lits au moment du calcul (0–100). */
    @Column(name = "occupancy_rate_percent")
    private Double occupancyRatePercent;

    /** Lits libres au moment du calcul. */
    @Column(name = "available_beds")
    private Integer availableBeds;

    /** Lits occupés au moment du calcul. */
    @Column(name = "occupied_beds")
    private Integer occupiedBeds;

    /** Total des lits déclarés dans hospital-service au moment du calcul. */
    @Column(name = "total_beds")
    private Integer totalBeds;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public AdmissionStats() {
    }

    // ── Getters / Setters ──

    public Long getId() { return id; }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

    public int getAdmissionsCount() { return admissionsCount; }
    public void setAdmissionsCount(int admissionsCount) { this.admissionsCount = admissionsCount; }

    public int getDischargesCount() { return dischargesCount; }
    public void setDischargesCount(int dischargesCount) { this.dischargesCount = dischargesCount; }

    public int getTransfersCount() { return transfersCount; }
    public void setTransfersCount(int transfersCount) { this.transfersCount = transfersCount; }

    public int getDeathsCount() { return deathsCount; }
    public void setDeathsCount(int deathsCount) { this.deathsCount = deathsCount; }

    public int getActiveAdmissions() { return activeAdmissions; }
    public void setActiveAdmissions(int activeAdmissions) { this.activeAdmissions = activeAdmissions; }

    public Double getAvgStayDays() { return avgStayDays; }
    public void setAvgStayDays(Double avgStayDays) { this.avgStayDays = avgStayDays; }

    public Double getOccupancyRatePercent() { return occupancyRatePercent; }
    public void setOccupancyRatePercent(Double occupancyRatePercent) { this.occupancyRatePercent = occupancyRatePercent; }

    public Integer getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(Integer availableBeds) { this.availableBeds = availableBeds; }

    public Integer getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(Integer occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public Integer getTotalBeds() { return totalBeds; }
    public void setTotalBeds(Integer totalBeds) { this.totalBeds = totalBeds; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
