package com.afya.platform.careentry.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.careentry.dto.AdmissionCreateRequest;
import com.afya.platform.careentry.integration.CatalogServiceClient;
import com.afya.platform.careentry.integration.HospitalServiceSummary;
import com.afya.platform.careentry.integration.PatientServiceClient;
import com.afya.platform.careentry.integration.PatientSummary;
import com.afya.platform.careentry.integration.StayServiceClient;
import com.afya.platform.careentry.model.Admission;
import com.afya.platform.careentry.repository.AdmissionRepository;
import com.afya.platform.careentry.repository.TransferRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceAuditTest {

    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private TransferRequestRepository transferRequestRepository;
    @Mock
    private PatientServiceClient patientServiceClient;
    @Mock
    private CatalogServiceClient catalogServiceClient;
    @Mock
    private StayServiceClient stayServiceClient;
    @Mock
    private AdmissionWriter admissionWriter;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private AdmissionService admissionService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void admitPublishesAdmissionCreated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reception1", null, List.of()));

        when(patientServiceClient.getPatient(eq(1L), any()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-1"));
        when(catalogServiceClient.getHospitalService(eq(2L), any()))
                .thenReturn(new HospitalServiceSummary(2L, "Médecine", 10, true));
        when(catalogServiceClient.resolveBedAssignment(eq(2L), isNull(), isNull(), any()))
                .thenReturn(new CatalogServiceClient.BedAssignment("A1", "A1-01"));
        when(admissionRepository.existsByPatientIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(admissionWriter.persist(any(Admission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 2L, null, null, null, null);
        admissionService.admit(request, "Bearer token");

        verify(stayServiceClient).open(any(), eq("Bearer token"));
        verify(auditEventPublisher).publish(
                eq("ADMISSION_CREATED"),
                eq("ADMISSION"),
                isNull(),
                eq("reception1"),
                org.mockito.ArgumentMatchers.contains("patientId"));
    }
}
