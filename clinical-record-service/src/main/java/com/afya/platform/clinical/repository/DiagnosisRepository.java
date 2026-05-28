package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    List<Diagnosis> findByMedicalRecordIdOrderByRecordedAtDesc(Long medicalRecordId);
}
