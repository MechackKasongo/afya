package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.VitalSignReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalSignReadingRepository extends JpaRepository<VitalSignReading, Long> {

    List<VitalSignReading> findByAdmissionIdOrderByRecordedAtDesc(Long admissionId);

    long countByRecordedAtBetween(java.time.Instant from, java.time.Instant to);
}
