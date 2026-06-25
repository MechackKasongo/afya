package com.afya.platform.admission.careentry.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.admission.dto.AdmissionCreateRequest;
import com.afya.platform.admission.dto.TransferRequestDto;
import com.afya.platform.admission.integration.CatalogServiceClient;
import com.afya.platform.admission.integration.HospitalServiceSummary;
import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.model.Admission;
import com.afya.platform.admission.model.AdmissionStatus;
import com.afya.platform.admission.repository.AdmissionRepository;
import com.afya.platform.admission.repository.TransferRequestRepository;
import com.afya.platform.admission.service.AdmissionLifecycleService;
import com.afya.platform.admission.service.AdmissionService;
import com.afya.platform.admission.service.AdmissionWriter;
import com.afya.platform.admission.stay.dto.StayRelocateRequest;
import com.afya.platform.admission.stay.dto.StayResponse;
import com.afya.platform.admission.stay.service.StayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
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
    private StayService stayService;
    @Mock
    private AdmissionWriter admissionWriter;
    @Mock
    private AuditEventPublisher auditEventPublisher;
    @Mock
    private AdmissionLifecycleService admissionLifecycleService;

    private AdmissionService admissionService;

    @BeforeEach
    void setUp() {
        admissionService = new AdmissionService(
                admissionRepository,
                transferRequestRepository,
                patientServiceClient,
                catalogServiceClient,
                stayService,
                admissionWriter,
                auditEventPublisher,
                admissionLifecycleService);
    }

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
        when(admissionWriter.persist(any(Admission.class))).thenAnswer(invocation -> {
            Admission admission = invocation.getArgument(0);
            setAdmissionId(admission, 100L);
            return admission;
        });
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionCreateRequest request = new AdmissionCreateRequest(1L, 2L, null, null, null, null);
        admissionService.admit(request, "Bearer token");

        verify(stayService).open(any(), eq("Bearer token"));
        verify(admissionLifecycleService).notifyHospitalisation(any());
        verify(auditEventPublisher).publish(
                eq("ADMISSION_CREATED"),
                eq("ADMISSION"),
                eq("100"),
                eq("reception1"),
                org.mockito.ArgumentMatchers.contains("patientId"));
    }

    @Test
    void transferReleasesOldBedAndOccupiesNewBed() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("medecin1", null, List.of()));

        Admission admission = mock(Admission.class);
        when(admission.getId()).thenReturn(10L);
        when(admission.getPatientId()).thenReturn(1L);
        when(admission.getHospitalServiceId()).thenReturn(2L);
        when(admission.getStatus()).thenReturn(AdmissionStatus.OUVERTE);

        when(admissionRepository.findById(10L)).thenReturn(Optional.of(admission));
        when(catalogServiceClient.getHospitalService(eq(3L), any()))
                .thenReturn(new HospitalServiceSummary(3L, "Chirurgie", 8, true));
        when(stayService.getByAdmissionId(eq(10L), any()))
                .thenReturn(new StayResponse(
                        5L, 1L, "Jean Mukendi", "DOS-1", 10L,
                        Instant.now(), null, "A1", "01", "EN_COURS"));
        when(catalogServiceClient.resolveBedAssignment(eq(3L), isNull(), isNull(), any()))
                .thenReturn(new CatalogServiceClient.BedAssignment("B2", "02"));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patientServiceClient.getPatient(eq(1L), any()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-1"));

        admissionService.transfer(10L, new TransferRequestDto(3L, "Transfert chirurgical"), "Bearer token");

        verify(catalogServiceClient).updateBedOccupancy(
                eq(2L), eq("A1"), eq("01"), eq(false), eq(1L), eq(10L), any());
        verify(stayService).relocateByAdmissionId(
                eq(10L), eq(new StayRelocateRequest("B2", "02")), eq("Bearer token"));
        verify(catalogServiceClient).updateBedOccupancy(
                eq(3L), eq("B2"), eq("02"), eq(true), eq(1L), eq(10L), any());
        verify(admissionLifecycleService).notifyTransfer(10L);
    }

    private static void setAdmissionId(Admission admission, Long id) {
        try {
            Field field = Admission.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(admission, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
