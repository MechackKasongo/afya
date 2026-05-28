package com.afya.platform.patient.service;

import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.patient.dto.PatientCreateRequest;
import com.afya.platform.patient.model.Patient;
import com.afya.platform.patient.repository.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientRegistryServiceAuditTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DossierNumberGenerator dossierNumberGenerator;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private PatientRegistryService patientRegistryService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPublishesPatientCreated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reception1", null, List.of()));

        PatientCreateRequest request = new PatientCreateRequest(
                "Marie", "Kabila", null, null, LocalDate.of(1990, 5, 12), "F",
                "+243900000001", null, null, null, null);

        when(dossierNumberGenerator.generate()).thenReturn("D2026TEST001");
        when(patientRepository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDateAndSex(
                any(), any(), any(), any())).thenReturn(false);

        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patientRegistryService.create(request);

        verify(auditEventPublisher).publish(
                eq("PATIENT_CREATED"),
                eq("PATIENT"),
                isNull(),
                eq("reception1"),
                org.mockito.ArgumentMatchers.contains("D2026TEST001"));
    }
}
