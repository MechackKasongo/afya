package com.afya.platform.medical.repository;

import com.afya.platform.medical.model.ConsultationEvent;
import com.afya.platform.medical.model.ConsultationEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationEventRepository extends JpaRepository<ConsultationEvent, Long> {

    List<ConsultationEvent> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<ConsultationEvent> findByConsultation_IdOrderByCreatedAtDesc(Long consultationId);

    Optional<ConsultationEvent> findFirstByExamRequestIdAndEventType(Long examRequestId, ConsultationEventType eventType);

    boolean existsByExamRequestIdAndEventType(Long examRequestId, ConsultationEventType eventType);
}
