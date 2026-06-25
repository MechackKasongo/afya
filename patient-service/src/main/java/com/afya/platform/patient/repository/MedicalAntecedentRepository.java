package com.afya.platform.patient.repository;

import com.afya.platform.patient.model.MedicalAntecedent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalAntecedentRepository extends JpaRepository<MedicalAntecedent, Long> {

    List<MedicalAntecedent> findByPatientIdOrderByEventDateDescCreatedAtDesc(Long patientId);

    Optional<MedicalAntecedent> findByIdAndPatientId(Long id, Long patientId);
}
