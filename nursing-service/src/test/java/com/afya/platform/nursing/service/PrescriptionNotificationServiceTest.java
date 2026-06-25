package com.afya.platform.nursing.service;

import com.afya.platform.nursing.dto.CreatePrescriptionNotificationRequest;
import com.afya.platform.nursing.model.PrescriptionNotification;
import com.afya.platform.nursing.model.PrescriptionNotificationStatus;
import com.afya.platform.nursing.repository.PrescriptionNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionNotificationServiceTest {

    @Mock
    private PrescriptionNotificationRepository prescriptionNotificationRepository;

    @InjectMocks
    private PrescriptionNotificationService prescriptionNotificationService;

    @Test
    void createPersistsNotificationWhenAbsent() {
        when(prescriptionNotificationRepository.findByPrescriptionLineId(10L)).thenReturn(Optional.empty());
        when(prescriptionNotificationRepository.save(any(PrescriptionNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = prescriptionNotificationService.create(
                new CreatePrescriptionNotificationRequest(10L, 3L, "Paracétamol"));

        assertThat(response.prescriptionLineId()).isEqualTo(10L);
        assertThat(response.patientId()).isEqualTo(3L);
        assertThat(response.drugName()).isEqualTo("Paracétamol");
        assertThat(response.status()).isEqualTo(PrescriptionNotificationStatus.ENVOYEE);

        ArgumentCaptor<PrescriptionNotification> captor = ArgumentCaptor.forClass(PrescriptionNotification.class);
        verify(prescriptionNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getDrugName()).isEqualTo("Paracétamol");
    }

    @Test
    void createIsIdempotentForSamePrescriptionLine() {
        PrescriptionNotification existing = new PrescriptionNotification();
        existing.setPrescriptionLineId(10L);
        existing.setPatientId(3L);
        existing.setDrugName("Paracétamol");
        when(prescriptionNotificationRepository.findByPrescriptionLineId(10L)).thenReturn(Optional.of(existing));

        var response = prescriptionNotificationService.create(
                new CreatePrescriptionNotificationRequest(10L, 3L, "Paracétamol"));

        assertThat(response.prescriptionLineId()).isEqualTo(10L);
        verify(prescriptionNotificationRepository, never()).save(any());
    }

    @Test
    void markExecutedUpdatesStatus() {
        PrescriptionNotification notification = new PrescriptionNotification();
        notification.setPrescriptionLineId(10L);
        notification.setPatientId(3L);
        notification.setDrugName("Paracétamol");
        when(prescriptionNotificationRepository.findByPrescriptionLineId(10L)).thenReturn(Optional.of(notification));
        when(prescriptionNotificationRepository.save(any(PrescriptionNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        prescriptionNotificationService.markExecuted(10L, 99L, "infirmiere1");

        ArgumentCaptor<PrescriptionNotification> captor = ArgumentCaptor.forClass(PrescriptionNotification.class);
        verify(prescriptionNotificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PrescriptionNotificationStatus.EXECUTEE);
        assertThat(captor.getValue().getMedicationAdministrationId()).isEqualTo(99L);
        assertThat(captor.getValue().getNurseUsername()).isEqualTo("infirmiere1");
    }
}
