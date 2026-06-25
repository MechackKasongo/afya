package com.afya.platform.admission.service;

import com.afya.platform.admission.dto.DischargeRecordResponse;
import com.afya.platform.admission.model.AdmissionNotificationType;
import com.afya.platform.admission.model.DischargeRecord;
import com.afya.platform.admission.model.DischargeType;
import com.afya.platform.admission.repository.AdmissionNotificationRepository;
import com.afya.platform.admission.repository.DischargeRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionLifecycleServiceTest {

    @Mock
    private DischargeRecordRepository dischargeRecordRepository;
    @Mock
    private AdmissionNotificationRepository admissionNotificationRepository;

    @InjectMocks
    private AdmissionLifecycleService admissionLifecycleService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordDischargePersistsDischargeAndNotification() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medecin1", null, List.of()));

        when(dischargeRecordRepository.findByAdmissionId(5L)).thenReturn(Optional.empty());
        when(dischargeRecordRepository.save(any(DischargeRecord.class))).thenAnswer(invocation -> {
            DischargeRecord record = invocation.getArgument(0);
            return record;
        });

        Instant dischargedAt = Instant.parse("2026-06-01T10:00:00Z");
        DischargeRecordResponse response = admissionLifecycleService.recordDischarge(
                5L,
                DischargeType.GUERI,
                "Repos 7 jours",
                dischargedAt);

        assertThat(response.admissionId()).isEqualTo(5L);
        assertThat(response.dischargeType()).isEqualTo(DischargeType.GUERI);
        assertThat(response.recordedByUsername()).isEqualTo("medecin1");

        ArgumentCaptor<com.afya.platform.admission.model.AdmissionNotification> captor =
                ArgumentCaptor.forClass(com.afya.platform.admission.model.AdmissionNotification.class);
        verify(admissionNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(AdmissionNotificationType.SORTIE_AUTORISEE);
    }
}
