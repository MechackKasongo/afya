package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.PrescriptionNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.afya.platform.nursing.model.PrescriptionNotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PrescriptionNotificationRepository extends JpaRepository<PrescriptionNotification, Long> {

    List<PrescriptionNotification> findByPatientIdOrderBySentAtDesc(Long patientId);

    Optional<PrescriptionNotification> findByIdAndPatientId(Long id, Long patientId);

    Optional<PrescriptionNotification> findByPrescriptionLineId(Long prescriptionLineId);

    long countBySentAtBetween(Instant from, Instant to);

    long countByStatusAndExecutedAtBetween(
            PrescriptionNotificationStatus status,
            Instant from,
            Instant to
    );
}
