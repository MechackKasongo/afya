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
 * Statistiques pré-agrégées de l'activité médicale — MD-09 (StatistiqueMedical du mémoire).
 * Alimentée quotidiennement par {@code AdmissionStatsScheduler}.
 *
 * <p>Indicateurs médicaux journaliers :</p>
 * <ul>
 *   <li>Consultations du jour</li>
 *   <li>Prescriptions créées</li>
 *   <li>Diagnostics posés</li>
 *   <li>Examens demandés au laboratoire</li>
 *   <li>Soins infirmiers effectués</li>
 * </ul>
 */
@Entity
@Table(name = "medical_stats")
public class MedicalStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Date de référence des statistiques (jour J). */
    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    /** Nombre de consultations créées ce jour (via audit-service). */
    @Column(name = "consultations_count", nullable = false)
    private int consultationsCount;

    /** Nombre de prescriptions créées ce jour. */
    @Column(name = "prescriptions_count", nullable = false)
    private int prescriptionsCount;

    /** Nombre de diagnostics posés ce jour. */
    @Column(name = "diagnoses_count", nullable = false)
    private int diagnosesCount;

    /** Nombre de demandes d'examen labo créées ce jour. */
    @Column(name = "exam_requests_count", nullable = false)
    private int examRequestsCount;

    /** Nombre de soins infirmiers enregistrés ce jour. */
    @Column(name = "nursing_care_count", nullable = false)
    private int nursingCareCount;

    /** Total événements d'audit pour la journée. */
    @Column(name = "total_audit_events", nullable = false)
    private int totalAuditEvents;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    public MedicalStats() {
    }

    // ── Getters / Setters ──

    public Long getId() { return id; }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

    public int getConsultationsCount() { return consultationsCount; }
    public void setConsultationsCount(int consultationsCount) { this.consultationsCount = consultationsCount; }

    public int getPrescriptionsCount() { return prescriptionsCount; }
    public void setPrescriptionsCount(int prescriptionsCount) { this.prescriptionsCount = prescriptionsCount; }

    public int getDiagnosesCount() { return diagnosesCount; }
    public void setDiagnosesCount(int diagnosesCount) { this.diagnosesCount = diagnosesCount; }

    public int getExamRequestsCount() { return examRequestsCount; }
    public void setExamRequestsCount(int examRequestsCount) { this.examRequestsCount = examRequestsCount; }

    public int getNursingCareCount() { return nursingCareCount; }
    public void setNursingCareCount(int nursingCareCount) { this.nursingCareCount = nursingCareCount; }

    public int getTotalAuditEvents() { return totalAuditEvents; }
    public void setTotalAuditEvents(int totalAuditEvents) { this.totalAuditEvents = totalAuditEvents; }

    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
}
