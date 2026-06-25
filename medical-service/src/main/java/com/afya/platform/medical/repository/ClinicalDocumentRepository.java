package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.ClinicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, Long> {

    List<ClinicalDocument> findByMedicalRecordIdOrderByUploadedAtDesc(Long medicalRecordId);

    Optional<ClinicalDocument> findByIdAndMedicalRecord_PatientId(Long id, Long patientId);
}
