package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.PrescriptionNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionNotificationRepository extends JpaRepository<PrescriptionNotification, Long> {

    List<PrescriptionNotification> findByPatientIdOrderBySentAtDesc(Long patientId);

    Optional<PrescriptionNotification> findByIdAndPatientId(Long id, Long patientId);

    Optional<PrescriptionNotification> findByPrescriptionLineId(Long prescriptionLineId);
}
