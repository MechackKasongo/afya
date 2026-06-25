package com.afya.platform.admission.service;

import com.afya.platform.admission.dto.EmergencyTimelineEventResponse;
import com.afya.platform.admission.model.EmergencyStatus;
import com.afya.platform.admission.model.EmergencyVisit;
import com.afya.platform.admission.model.EmergencyVisitTimelineEvent;
import com.afya.platform.admission.repository.EmergencyVisitTimelineEventRepository;
import com.afya.platform.admission.repository.EmergencyVisitRepository;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmergencyVisitTimelineService {

    private final EmergencyVisitRepository emergencyVisitRepository;
    private final EmergencyVisitTimelineEventRepository timelineEventRepository;

    public EmergencyVisitTimelineService(
            EmergencyVisitRepository emergencyVisitRepository,
            EmergencyVisitTimelineEventRepository timelineEventRepository
    ) {
        this.emergencyVisitRepository = emergencyVisitRepository;
        this.timelineEventRepository = timelineEventRepository;
    }

    public List<EmergencyTimelineEventResponse> listForVisit(Long emergencyVisitId) {
        EmergencyVisit visit = emergencyVisitRepository.findById(emergencyVisitId)
                .orElseThrow(() -> new NotFoundException("Passage urgences introuvable : " + emergencyVisitId));

        List<EmergencyTimelineEventResponse> persisted = timelineEventRepository
                .findByEmergencyVisit_IdOrderByCreatedAtAsc(emergencyVisitId).stream()
                .map(event -> toResponse(event, emergencyVisitId))
                .toList();

        if (!persisted.isEmpty()) {
            return persisted;
        }
        return synthesizeFromVisit(visit);
    }

    @Transactional
    public void record(Long emergencyVisitId, String eventType, String details) {
        EmergencyVisit visit = emergencyVisitRepository.findById(emergencyVisitId)
                .orElseThrow(() -> new NotFoundException("Passage urgences introuvable : " + emergencyVisitId));
        EmergencyVisitTimelineEvent event = new EmergencyVisitTimelineEvent();
        event.setEmergencyVisit(visit);
        event.setEventType(eventType);
        event.setDetails(blankToNull(details));
        timelineEventRepository.save(event);
    }

    private static List<EmergencyTimelineEventResponse> synthesizeFromVisit(EmergencyVisit visit) {
        List<EmergencyTimelineEventResponse> events = new ArrayList<>();
        long syntheticId = -1;
        Instant arrivedAt = visit.getArrivedAt() != null ? visit.getArrivedAt() : Instant.now();

        events.add(new EmergencyTimelineEventResponse(
                syntheticId--,
                visit.getId(),
                "ARRIVEE",
                formatArrivalDetails(visit.getPriority(), visit.getTriageNotes()),
                arrivedAt));

        if (visit.getTriageLevel() != null && !visit.getTriageLevel().isBlank()) {
            events.add(new EmergencyTimelineEventResponse(
                    syntheticId--,
                    visit.getId(),
                    "TRIAGE",
                    "Niveau " + visit.getTriageLevel().strip(),
                    arrivedAt.plusSeconds(1)));
        }

        if (visit.getOrientation() != null && !visit.getOrientation().isBlank()) {
            events.add(new EmergencyTimelineEventResponse(
                    syntheticId--,
                    visit.getId(),
                    "ORIENTATION",
                    visit.getOrientation().strip(),
                    arrivedAt.plusSeconds(2)));
        }

        if (visit.getStatus() == EmergencyStatus.CLOTURE) {
            Instant closedAt = visit.getEndedAt() != null ? visit.getEndedAt() : arrivedAt.plusSeconds(3);
            events.add(new EmergencyTimelineEventResponse(
                    syntheticId,
                    visit.getId(),
                    "CLOTURE",
                    "Passage clôturé",
                    closedAt));
        }

        return events;
    }

    private static String formatArrivalDetails(String priority, String motif) {
        StringBuilder sb = new StringBuilder("Priorité ").append(priority != null ? priority : "P2");
        if (motif != null && !motif.isBlank()) {
            sb.append(" — Motif : ").append(motif.strip());
        }
        return sb.toString();
    }

    private static EmergencyTimelineEventResponse toResponse(EmergencyVisitTimelineEvent event, Long emergencyVisitId) {
        return new EmergencyTimelineEventResponse(
                event.getId(),
                emergencyVisitId,
                event.getEventType(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
