package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.ClinicalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, Long> {

    List<ClinicalDocument> findByMedicalRecordIdOrderByUploadedAtDesc(Long medicalRecordId);

    Optional<ClinicalDocument> findByIdAndMedicalRecord_PatientId(Long id, Long patientId);
}
