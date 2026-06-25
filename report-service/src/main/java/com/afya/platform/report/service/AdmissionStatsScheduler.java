package com.afya.platform.report.service;

import com.afya.platform.report.dto.ActivityCountItem;
import com.afya.platform.report.dto.ActivityReportResponse;
import com.afya.platform.report.model.AdmissionStats;
import com.afya.platform.report.model.MedicalStats;
import com.afya.platform.report.repository.AdmissionStatsRepository;
import com.afya.platform.report.repository.MedicalStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scheduler de pré-agrégation des statistiques — MD-09.
 *
 * <p>Calcule et persiste quotidiennement :</p>
 * <ul>
 *   <li>{@link AdmissionStats} : admissions/jour, durée moyenne séjour, taux occupation lits</li>
 *   <li>{@link MedicalStats} : consultations, prescriptions, diagnostics, examens, soins</li>
 * </ul>
 *
 * <p>L'agrégation s'appuie sur {@link ActivityReportService} qui interroge l'audit-service.
 * En cas d'indisponibilité (mode dégradé), les compteurs restent à 0 mais l'entrée est
 * tout de même persistée pour ne pas manquer un jour dans la série temporelle.</p>
 */
@Service
public class AdmissionStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdmissionStatsScheduler.class);

    private final AdmissionStatsRepository admissionStatsRepository;
    private final MedicalStatsRepository medicalStatsRepository;
    private final ActivityReportService activityReportService;

    public AdmissionStatsScheduler(
            AdmissionStatsRepository admissionStatsRepository,
            MedicalStatsRepository medicalStatsRepository,
            ActivityReportService activityReportService
    ) {
        this.admissionStatsRepository = admissionStatsRepository;
        this.medicalStatsRepository = medicalStatsRepository;
        this.activityReportService = activityReportService;
    }

    /**
     * Calcule les statistiques du jour J−1 chaque nuit à 02:00 UTC.
     * La cron s'exécute à 02:00 UTC tous les jours.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void computeYesterdayStats() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        log.info("[AdmissionStatsScheduler] Calcul statistiques pour {}", yesterday);
        computeStatsFor(yesterday);
    }

    /**
     * Calcule les statistiques du jour courant à 23:55 UTC (snapshot fin de journée).
     */
    @Scheduled(cron = "0 55 23 * * *", zone = "UTC")
    @Transactional
    public void computeTodaySnapshot() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        log.info("[AdmissionStatsScheduler] Snapshot statistiques pour {}", today);
        computeStatsFor(today);
    }

    /**
     * Calcule et persiste les statistiques pour une date donnée (idempotent : upsert).
     */
    @Transactional
    public void computeStatsFor(LocalDate date) {
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        ActivityReportResponse activity = fetchActivity(from, to);
        computeAdmissionStats(date, activity);
        computeMedicalStats(date, activity);
    }

    // ── Calcul AdmissionStats ──────────────────────────────────────────────────

    private void computeAdmissionStats(LocalDate date, ActivityReportResponse activity) {
        AdmissionStats stats = admissionStatsRepository.findByStatDate(date)
                .orElseGet(AdmissionStats::new);

        stats.setStatDate(date);
        stats.setComputedAt(Instant.now());

        if (activity != null && !activity.degraded()) {
            stats.setAdmissionsCount(countAction(activity, "ADMISSION_CREATED"));
            stats.setDischargesCount(countAction(activity, "ADMISSION_DISCHARGED")
                    + countAction(activity, "DISCHARGE_RECORD_CREATED"));
            stats.setTransfersCount(countAction(activity, "TRANSFER_REQUEST_CREATED")
                    + countAction(activity, "ADMISSION_TRANSFERRED"));
            stats.setDeathsCount(countAction(activity, "DEATH_DECLARED")
                    + countAction(activity, "ADMISSION_DECEASED"));
        } else {
            // Mode dégradé : on ne réinitialise pas les valeurs existantes
            if (stats.getId() == null) {
                stats.setAdmissionsCount(0);
                stats.setDischargesCount(0);
                stats.setTransfersCount(0);
                stats.setDeathsCount(0);
            }
            log.warn("[AdmissionStatsScheduler] Mode dégradé pour {} — compteurs admissions à 0", date);
        }

        admissionStatsRepository.save(stats);
        log.debug("[AdmissionStatsScheduler] AdmissionStats {} sauvegardé (admissions={})",
                date, stats.getAdmissionsCount());
    }

    // ── Calcul MedicalStats ────────────────────────────────────────────────────

    private void computeMedicalStats(LocalDate date, ActivityReportResponse activity) {
        MedicalStats stats = medicalStatsRepository.findByStatDate(date)
                .orElseGet(MedicalStats::new);

        stats.setStatDate(date);
        stats.setComputedAt(Instant.now());

        if (activity != null && !activity.degraded()) {
            stats.setTotalAuditEvents((int) activity.totalEvents());
            stats.setConsultationsCount(countAction(activity, "CONSULTATION_CREATED"));
            stats.setPrescriptionsCount(countAction(activity, "PRESCRIPTION_CREATED")
                    + countAction(activity, "PRESCRIPTION_LINE_CREATED"));
            stats.setDiagnosesCount(countAction(activity, "DIAGNOSIS_RECORDED")
                    + countAction(activity, "CONSULTATION_EVENT_DIAGNOSIS"));
            stats.setExamRequestsCount(countAction(activity, "EXAM_REQUEST_CREATED"));
            stats.setNursingCareCount(countAction(activity, "NURSING_CARE_RECORD_CREATED")
                    + countAction(activity, "MEDICATION_ADMINISTERED"));
        } else {
            if (stats.getId() == null) {
                stats.setTotalAuditEvents(0);
                stats.setConsultationsCount(0);
                stats.setPrescriptionsCount(0);
                stats.setDiagnosesCount(0);
                stats.setExamRequestsCount(0);
                stats.setNursingCareCount(0);
            }
            log.warn("[AdmissionStatsScheduler] Mode dégradé pour {} — compteurs médicaux à 0", date);
        }

        medicalStatsRepository.save(stats);
        log.debug("[AdmissionStatsScheduler] MedicalStats {} sauvegardé (consultations={})",
                date, stats.getConsultationsCount());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ActivityReportResponse fetchActivity(Instant from, Instant to) {
        try {
            ActivityReportResponse response = activityReportService.activityReport(from, to);
            if (response.degraded()) {
                log.warn("[AdmissionStatsScheduler] audit-service dégradé — stats partielles");
            }
            return response;
        } catch (RestClientException ex) {
            log.error("[AdmissionStatsScheduler] Impossible de joindre audit-service : {}", ex.getMessage());
            return null;
        }
    }

    private static int countAction(ActivityReportResponse activity, String action) {
        if (activity == null || activity.byAction() == null) return 0;
        return (int) activity.byAction().stream()
                .filter(item -> action.equalsIgnoreCase(item.key()))
                .mapToLong(ActivityCountItem::count)
                .sum();
    }
}
