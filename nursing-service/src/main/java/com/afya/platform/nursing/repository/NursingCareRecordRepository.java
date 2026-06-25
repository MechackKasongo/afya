package com.afya.platform.nursing.repository;

import com.afya.platform.nursing.model.NursingCareRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NursingCareRecordRepository extends JpaRepository<NursingCareRecord, Long> {

    List<NursingCareRecord> findByMedicalRecordIdOrderByPerformedAtDesc(Long medicalRecordId);
}
