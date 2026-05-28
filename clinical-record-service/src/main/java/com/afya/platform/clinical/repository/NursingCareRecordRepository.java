package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.NursingCareRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NursingCareRecordRepository extends JpaRepository<NursingCareRecord, Long> {

    List<NursingCareRecord> findByMedicalRecordIdOrderByPerformedAtDesc(Long medicalRecordId);
}
