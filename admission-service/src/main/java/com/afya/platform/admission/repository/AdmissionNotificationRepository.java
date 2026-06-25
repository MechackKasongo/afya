package com.afya.platform.admission.repository;

import com.afya.platform.admission.model.AdmissionNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdmissionNotificationRepository extends JpaRepository<AdmissionNotification, Long> {

    List<AdmissionNotification> findByAdmissionIdOrderBySentAtDesc(Long admissionId);

    Optional<AdmissionNotification> findByIdAndAdmissionId(Long id, Long admissionId);
}
