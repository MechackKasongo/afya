package com.afya.platform.clinical.repository;

import com.afya.platform.clinical.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    List<ClinicalNote> findByMedicalRecordIdOrderByAuthoredAtDesc(Long medicalRecordId);
}
