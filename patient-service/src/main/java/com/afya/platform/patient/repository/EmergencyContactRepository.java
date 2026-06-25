package com.afya.platform.patient.repository;

import com.afya.platform.patient.model.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByPatientIdOrderByCreatedAtAsc(Long patientId);

    Optional<EmergencyContact> findByIdAndPatientId(Long id, Long patientId);
}
