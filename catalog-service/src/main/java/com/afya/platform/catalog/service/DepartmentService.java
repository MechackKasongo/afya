package com.afya.platform.catalog.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.catalog.dto.DepartmentRequest;
import com.afya.platform.catalog.dto.DepartmentResponse;
import com.afya.platform.catalog.model.Department;
import com.afya.platform.catalog.repository.DepartmentRepository;
import com.afya.platform.catalog.support.DepartmentCodeGenerator;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuditEventPublisher auditEventPublisher;

    public DepartmentService(DepartmentRepository departmentRepository, AuditEventPublisher auditEventPublisher) {
        this.departmentRepository = departmentRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public DepartmentResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String code = resolveUniqueCode(request.code(), request.name().strip());
        Department department = new Department();
        department.setCode(code);
        department.setName(request.name().strip());
        department.setActive(request.active() == null || request.active());
        Department saved = departmentRepository.save(department);
        publishDepartment("DEPARTMENT_CREATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = find(id);
        String code = resolveCodeForUpdate(department.getCode(), request.code(), request.name().strip());
        departmentRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(ignored -> {
                    throw new ConflictException("Code département déjà utilisé : " + code);
                });
        department.setCode(code);
        department.setName(request.name().strip());
        if (request.active() != null) {
            department.setActive(request.active());
        }
        Department saved = departmentRepository.save(department);
        publishDepartment("DEPARTMENT_UPDATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Department department = find(id);
        auditEventPublisher.publish(
                "DEPARTMENT_DELETED",
                "DEPARTMENT",
                AuditMetadata.resourceId(department.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("code", department.getCode()));
        departmentRepository.delete(department);
    }

    private void publishDepartment(String action, Department department) {
        auditEventPublisher.publish(
                action,
                "DEPARTMENT",
                AuditMetadata.resourceId(department.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("code", department.getCode()));
    }

    Department find(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Département introuvable : " + id));
    }

    private String resolveUniqueCode(String requestedCode, String name) {
        String base = normalizeRequestedCode(requestedCode, name);
        for (int suffix = 1; suffix <= 99; suffix++) {
            String candidate = DepartmentCodeGenerator.withSuffix(base, suffix);
            if (!departmentRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("Impossible de générer un code unique pour : " + name);
    }

    private static String resolveCodeForUpdate(String currentCode, String requestedCode, String name) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.strip().toUpperCase();
        }
        return currentCode;
    }

    private static String normalizeRequestedCode(String requestedCode, String name) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.strip().toUpperCase();
        }
        return DepartmentCodeGenerator.fromName(name);
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.isActive()
        );
    }
}
