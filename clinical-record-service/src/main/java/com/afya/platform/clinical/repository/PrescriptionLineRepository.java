package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.PrescriptionLine;
import com.afya.platform.clinical.model.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLine, Long> {

    List<PrescriptionLine> findByMedicalRecordIdOrderByCreatedAtDesc(Long medicalRecordId);

    List<PrescriptionLine> findByMedicalRecordIdAndStatusOrderByCreatedAtDesc(
            Long medicalRecordId,
            PrescriptionStatus status
    );
}
