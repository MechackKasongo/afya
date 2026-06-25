package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    List<Diagnosis> findByMedicalRecordIdOrderByRecordedAtDesc(Long medicalRecordId);
}
