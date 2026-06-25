package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    List<ClinicalNote> findByMedicalRecordIdOrderByAuthoredAtDesc(Long medicalRecordId);
}
