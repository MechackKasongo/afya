package com.afya.platform.careentry.repository;

import com.afya.platform.careentry.model.EmergencyVisitTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyVisitTimelineEventRepository extends JpaRepository<EmergencyVisitTimelineEvent, Long> {

    List<EmergencyVisitTimelineEvent> findByEmergencyVisit_IdOrderByCreatedAtAsc(Long emergencyVisitId);
}
