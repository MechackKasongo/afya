package com.afya.platform.lab.service;

import com.afya.platform.lab.dto.ExamRequestCreateRequest;
import com.afya.platform.lab.dto.ExamRequestResponse;
import com.afya.platform.lab.dto.ExamTypeRequest;
import com.afya.platform.lab.dto.ExamTypeResponse;
import com.afya.platform.lab.model.ExamCategory;
import com.afya.platform.lab.model.ExamRequest;
import com.afya.platform.lab.model.ExamRequestStatus;
import com.afya.platform.lab.model.ExamType;
import com.afya.platform.lab.model.ExamUrgency;
import com.afya.platform.lab.repository.ExamRequestRepository;
import com.afya.platform.lab.repository.ExamResultRepository;
import com.afya.platform.lab.repository.ExamTypeRepository;
import com.afya.platform.lab.repository.SpecimenCollectionRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du service laboratoire — lifecycle ExamRequest.
 */
@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock ExamTypeRepository examTypeRepository;
    @Mock ExamRequestRepository examRequestRepository;
    @Mock SpecimenCollectionRepository specimenCollectionRepository;
    @Mock ExamResultRepository examResultRepository;
    @Mock AuditEventPublisher auditEventPublisher;

    @InjectMocks
    LabService labService;

    private ExamType nfsType;

    @BeforeEach
    void setUp() {
        nfsType = new ExamType();
        nfsType.setName("NFS");
        nfsType.setCategory(ExamCategory.BIOLOGY);
        nfsType.setActive(true);
        // Simulate JPA-generated ID via reflection
        try {
            var f = ExamType.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(nfsType, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── ExamType ───────────────────────────────────────────────────────────────

    @Test
    void createExamType_success() {
        when(examTypeRepository.findByNameIgnoreCase("NFS")).thenReturn(Optional.empty());
        when(examTypeRepository.save(any(ExamType.class))).thenReturn(nfsType);

        ExamTypeRequest request = new ExamTypeRequest("NFS", "Numération Formule Sanguine", ExamCategory.BIOLOGY, null);
        ExamTypeResponse response = labService.createExamType(request);

        assertThat(response.name()).isEqualTo("NFS");
        assertThat(response.category()).isEqualTo(ExamCategory.BIOLOGY);
    }

    @Test
    void createExamType_duplicateName_throwsConflict() {
        when(examTypeRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.of(nfsType));

        ExamTypeRequest request = new ExamTypeRequest("NFS", null, ExamCategory.BIOLOGY, null);
        assertThatThrownBy(() -> labService.createExamType(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("NFS");
    }

    // ── ExamRequest ────────────────────────────────────────────────────────────

    @Test
    void createExamRequest_withValidType_persistsAndPublishes() {
        when(examTypeRepository.findById(1L)).thenReturn(Optional.of(nfsType));

        ExamRequest savedRequest = new ExamRequest();
        try {
            var f = ExamRequest.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(savedRequest, 10L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        savedRequest.setPatientId(42L);
        savedRequest.setDoctorId(7L);
        savedRequest.setUrgency(ExamUrgency.URGENT);
        savedRequest.setStatus(ExamRequestStatus.PENDING);

        when(examRequestRepository.save(any(ExamRequest.class))).thenReturn(savedRequest);

        ExamRequestCreateRequest request = new ExamRequestCreateRequest(42L, 7L, null, ExamUrgency.URGENT, null, List.of(1L));
        ExamRequestResponse response = labService.createRequest(request);

        assertThat(response.patientId()).isEqualTo(42L);
        assertThat(response.status()).isEqualTo(ExamRequestStatus.PENDING);
        verify(auditEventPublisher).publish("EXAM_REQUEST_CREATED", "ExamRequest", "10", "unknown", null);
    }

    @Test
    void createExamRequest_withInactiveType_throwsBadRequest() {
        nfsType.setActive(false);
        when(examTypeRepository.findById(1L)).thenReturn(Optional.of(nfsType));

        ExamRequestCreateRequest request = new ExamRequestCreateRequest(1L, 1L, null, ExamUrgency.NORMAL, null, List.of(1L));
        assertThatThrownBy(() -> labService.createRequest(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactif");
    }

    @Test
    void createExamRequest_withUnknownType_throwsNotFound() {
        when(examTypeRepository.findById(99L)).thenReturn(Optional.empty());

        ExamRequestCreateRequest request = new ExamRequestCreateRequest(1L, 1L, null, ExamUrgency.NORMAL, null, List.of(99L));
        assertThatThrownBy(() -> labService.createRequest(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void recordSpecimen_whenNotPending_throwsBadRequest() {
        ExamRequest existingRequest = new ExamRequest();
        existingRequest.setStatus(ExamRequestStatus.SPECIMEN_COLLECTED);
        when(examRequestRepository.findById(5L)).thenReturn(Optional.of(existingRequest));

        com.afya.platform.lab.dto.SpecimenCollectionRequest req =
                new com.afya.platform.lab.dto.SpecimenCollectionRequest(1L, "Sang");
        assertThatThrownBy(() -> labService.recordSpecimen(5L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SPECIMEN_COLLECTED");
    }
}
