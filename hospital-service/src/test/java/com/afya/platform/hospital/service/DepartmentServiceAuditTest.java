package com.afya.platform.hospital.service;

import com.afya.platform.hospital.dto.DepartmentRequest;
import com.afya.platform.hospital.model.Department;
import com.afya.platform.hospital.repository.DepartmentRepository;
import com.afya.platform.shared.audit.AuditEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceAuditTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private AuditEventPublisher auditEventPublisher;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository, auditEventPublisher);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPublishesDepartmentCreated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        when(departmentRepository.existsByCodeIgnoreCase("PEDIA")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        departmentService.create(new DepartmentRequest("PEDIA", "Pédiatrie", true));

        verify(auditEventPublisher).publish(
                eq("DEPARTMENT_CREATED"),
                eq("DEPARTMENT"),
                isNull(),
                eq("admin"),
                contains("PEDIA"));
    }

    @Test
    void deletePublishesDepartmentDeleted() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        Department department = mock(Department.class);
        when(department.getId()).thenReturn(3L);
        when(department.getCode()).thenReturn("URG");
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

        departmentService.delete(3L);

        verify(auditEventPublisher).publish(
                eq("DEPARTMENT_DELETED"),
                eq("DEPARTMENT"),
                eq("3"),
                eq("admin"),
                contains("URG"));
    }
}
