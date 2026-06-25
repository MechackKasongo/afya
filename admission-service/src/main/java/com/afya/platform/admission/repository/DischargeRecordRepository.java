package com.afya.platform.admission.repository;

import com.afya.platform.admission.model.DischargeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DischargeRecordRepository extends JpaRepository<DischargeRecord, Long> {

    Optional<DischargeRecord> findByAdmissionId(Long admissionId);
}
