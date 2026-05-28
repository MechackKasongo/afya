package com.afya.platform.careentry.repository;

import com.afya.platform.careentry.model.VitalSignReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VitalSignReadingRepository extends JpaRepository<VitalSignReading, Long> {

    List<VitalSignReading> findByAdmissionIdOrderByRecordedAtDesc(Long admissionId);
}
