package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.ConsultationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationEventRepository extends JpaRepository<ConsultationEvent, Long> {

    List<ConsultationEvent> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<ConsultationEvent> findByConsultation_IdOrderByCreatedAtDesc(Long consultationId);
}
