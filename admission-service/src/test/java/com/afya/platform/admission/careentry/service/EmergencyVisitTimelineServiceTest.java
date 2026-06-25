package com.afya.platform.admission.service;

import com.afya.platform.admission.dto.EmergencyTimelineEventResponse;
import com.afya.platform.admission.model.EmergencyStatus;
import com.afya.platform.admission.model.EmergencyVisit;
import com.afya.platform.admission.model.EmergencyVisitTimelineEvent;
import com.afya.platform.admission.repository.EmergencyVisitRepository;
import com.afya.platform.admission.repository.EmergencyVisitTimelineEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyVisitTimelineServiceTest {

    @Mock
    private EmergencyVisitRepository emergencyVisitRepository;

    @Mock
    private EmergencyVisitTimelineEventRepository timelineEventRepository;

    @InjectMocks
    private EmergencyVisitTimelineService timelineService;

    @Test
    void record_persistsEventForVisit() {
        EmergencyVisit visit = new EmergencyVisit();
        visit.setPatientId(10L);
        when(emergencyVisitRepository.findById(5L)).thenReturn(Optional.of(visit));
        when(timelineEventRepository.save(any())).thenAnswer(invocation -> {
            EmergencyVisitTimelineEvent event = invocation.getArgument(0);
            event.getClass();
            return event;
        });

        timelineService.record(5L, "TRIAGE", "Niveau 2");

        ArgumentCaptor<EmergencyVisitTimelineEvent> captor = ArgumentCaptor.forClass(EmergencyVisitTimelineEvent.class);
        verify(timelineEventRepository).save(captor.capture());
        assertEquals("TRIAGE", captor.getValue().getEventType());
        assertEquals("Niveau 2", captor.getValue().getDetails());
    }

    @Test
    void listForVisit_returnsMappedEvents() {
        EmergencyVisit visit = new EmergencyVisit();
        visit.setPatientId(10L);
        visit.setArrivedAt(Instant.parse("2026-05-27T10:00:00Z"));
        visit.setPriority("P2");
        visit.setStatus(EmergencyStatus.EN_ATTENTE_TRIAGE);
        when(emergencyVisitRepository.findById(3L)).thenReturn(Optional.of(visit));

        EmergencyVisitTimelineEvent event = new EmergencyVisitTimelineEvent();
        event.setEmergencyVisit(visit);
        event.setEventType("ARRIVEE");
        event.setDetails("Priorité P2");
        event.setCreatedAt(Instant.parse("2026-05-27T10:00:00Z"));
        when(timelineEventRepository.findByEmergencyVisit_IdOrderByCreatedAtAsc(3L)).thenReturn(List.of(event));

        List<EmergencyTimelineEventResponse> result = timelineService.listForVisit(3L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).urgenceId());
        assertEquals("ARRIVEE", result.get(0).type());
    }

    @Test
    void listForVisit_synthesizesWhenNoPersistedEvents() {
        EmergencyVisit visit = new EmergencyVisit();
        visit.setPatientId(10L);
        visit.setArrivedAt(Instant.parse("2026-05-27T10:00:00Z"));
        visit.setPriority("P1");
        visit.setTriageLevel("2");
        visit.setOrientation("Cardiologie");
        visit.setStatus(EmergencyStatus.ORIENTE);
        when(emergencyVisitRepository.findById(7L)).thenReturn(Optional.of(visit));
        when(timelineEventRepository.findByEmergencyVisit_IdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        List<EmergencyTimelineEventResponse> result = timelineService.listForVisit(7L);

        assertEquals(3, result.size());
        assertEquals("ARRIVEE", result.get(0).type());
        assertEquals("TRIAGE", result.get(1).type());
        assertEquals("ORIENTATION", result.get(2).type());
    }
}
