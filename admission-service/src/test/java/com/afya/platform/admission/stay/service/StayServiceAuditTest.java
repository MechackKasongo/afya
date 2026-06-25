package com.afya.platform.admission.stay.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.model.Admission;
import com.afya.platform.admission.model.AdmissionStatus;
import com.afya.platform.admission.repository.AdmissionRepository;
import com.afya.platform.admission.stay.dto.StayOpenRequest;
import com.afya.platform.admission.stay.model.Stay;
import com.afya.platform.admission.stay.model.StayStatus;
import com.afya.platform.admission.stay.repository.HospitalizationFormRepository;
import com.afya.platform.admission.stay.repository.StayRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayServiceAuditTest {

    @Mock
    private StayRepository stayRepository;
    @Mock
    private HospitalizationFormRepository hospitalizationFormRepository;
    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    private StayService stayService;

    @BeforeEach
    void setUp() {
        stayService = new StayService(
                stayRepository,
                hospitalizationFormRepository,
                admissionRepository,
                patientServiceClient,
                auditEventPublisher);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void openPublishesStayOpened() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reception1", null, List.of()));

        Admission admission = new Admission();
        admission.setPatientId(1L);
        admission.setHospitalServiceId(2L);
        admission.setAdmittedAt(Instant.now());
        admission.setStatus(AdmissionStatus.OUVERTE);

        when(stayRepository.existsByAdmissionId(10L)).thenReturn(false);
        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));
        when(stayRepository.existsByPatientIdAndStatusIn(eq(1L), any(EnumSet.class))).thenReturn(false);
        when(patientServiceClient.getPatient(eq(1L), any()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-1"));
        when(stayRepository.save(any(Stay.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hospitalizationFormRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StayOpenRequest request = new StayOpenRequest(10L, 1L, null, "A12", "LIT-3");
        stayService.open(request, "Bearer token");

        verify(auditEventPublisher).publish(
                eq("STAY_OPENED"), eq("STAY"), isNull(), eq("reception1"), org.mockito.ArgumentMatchers.contains("patientId"));
    }
}
