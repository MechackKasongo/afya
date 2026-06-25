package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.PrescriptionLine;
import com.afya.platform.medical.model.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLine, Long> {

    List<PrescriptionLine> findByMedicalRecordIdOrderByCreatedAtDesc(Long medicalRecordId);

    List<PrescriptionLine> findByMedicalRecordIdAndStatusOrderByCreatedAtDesc(
            Long medicalRecordId,
            PrescriptionStatus status
    );

    List<PrescriptionLine> findByAdmissionIdOrderByCreatedAtDesc(Long admissionId);
}
