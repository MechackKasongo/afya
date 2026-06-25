package com.afya.platform.hospital.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.hospital.dto.HospitalServiceRequest;
import com.afya.platform.hospital.dto.HospitalServiceResponse;
import com.afya.platform.hospital.dto.HospitalServiceStatusRequest;
import com.afya.platform.hospital.model.BedAssignmentPolicy;
import com.afya.platform.hospital.model.Department;
import com.afya.platform.hospital.model.HospitalService;
import com.afya.platform.hospital.support.BedAssignmentPolicySupport;
import com.afya.platform.hospital.support.RoomLetterPrefixSupport;
import com.afya.platform.hospital.repository.BedRepository;
import com.afya.platform.hospital.repository.HospitalServiceRepository;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalServiceCatalogService {

    private final HospitalServiceRepository hospitalServiceRepository;
    private final BedRepository bedRepository;
    private final DepartmentService departmentService;
    private final BedService bedService;
    private final AuditEventPublisher auditEventPublisher;

    public HospitalServiceCatalogService(
            HospitalServiceRepository hospitalServiceRepository,
            BedRepository bedRepository,
            DepartmentService departmentService,
            @Lazy BedService bedService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.bedRepository = bedRepository;
        this.departmentService = departmentService;
        this.bedService = bedService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<HospitalServiceResponse> list(Boolean activeOnly, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 100 ? 20 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
        Page<HospitalService> result = activeOnly != null && activeOnly
                ? hospitalServiceRepository.findByActive(true, pageable)
                : hospitalServiceRepository.findAll(pageable);
        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public HospitalServiceResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public HospitalServiceResponse create(HospitalServiceRequest request) {
        String name = request.name().strip();
        if (hospitalServiceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Service hospitalier déjà existant : " + name);
        }
        Department department = departmentService.find(request.departmentId());
        if (!department.isActive()) {
            throw new BadRequestException("Le département est inactif");
        }
        HospitalService service = new HospitalService();
        service.setDepartment(department);
        service.setName(name);
        service.setBedCapacity(request.bedCapacity());
        service.setBedsPerRoom(resolveBedsPerRoom(request.bedsPerRoom()));
        service.setRoomLetterPrefix(RoomLetterPrefixSupport.resolve(request.roomLetterPrefix(), 'A'));
        service.setBedAssignmentPolicy(BedAssignmentPolicySupport.resolve(
                request.bedAssignmentPolicy(), BedAssignmentPolicy.ROOM_ORDER_ASC));
        service.setActive(request.active() == null || request.active());
        HospitalService saved = hospitalServiceRepository.save(service);
        if (saved.getBedCapacity() > 0) {
            bedService.provisionMissingBeds(saved.getId());
        }
        publishHospitalService("HOSPITAL_SERVICE_CREATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public HospitalServiceResponse update(Long id, HospitalServiceRequest request) {
        HospitalService service = find(id);
        String name = request.name().strip();
        hospitalServiceRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(ignored -> {
                    throw new ConflictException("Service hospitalier déjà existant : " + name);
                });
        Department department = departmentService.find(request.departmentId());
        service.setDepartment(department);
        service.setName(name);
        service.setBedCapacity(request.bedCapacity());
        if (request.bedsPerRoom() != null) {
            service.setBedsPerRoom(resolveBedsPerRoom(request.bedsPerRoom()));
        }
        if (request.roomLetterPrefix() != null && !request.roomLetterPrefix().isBlank()) {
            service.setRoomLetterPrefix(RoomLetterPrefixSupport.resolve(request.roomLetterPrefix(), 'A'));
        }
        if (request.bedAssignmentPolicy() != null && !request.bedAssignmentPolicy().isBlank()) {
            service.setBedAssignmentPolicy(BedAssignmentPolicySupport.resolve(
                    request.bedAssignmentPolicy(), service.getBedAssignmentPolicy()));
        }
        if (request.active() != null) {
            service.setActive(request.active());
        }
        HospitalService saved = hospitalServiceRepository.save(service);
        if (saved.getBedCapacity() > 0) {
            bedService.provisionMissingBeds(saved.getId());
        }
        publishHospitalService("HOSPITAL_SERVICE_UPDATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public HospitalServiceResponse updateStatus(Long id, HospitalServiceStatusRequest request) {
        HospitalService service = find(id);
        service.setActive(request.active());
        HospitalService saved = hospitalServiceRepository.save(service);
        auditEventPublisher.publish(
                "HOSPITAL_SERVICE_STATUS_UPDATED",
                "HOSPITAL_SERVICE",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                "{\"active\":" + request.active() + ",\"departmentId\":" + saved.getDepartment().getId() + "}");
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        HospitalService service = find(id);
        auditEventPublisher.publish(
                "HOSPITAL_SERVICE_DELETED",
                "HOSPITAL_SERVICE",
                AuditMetadata.resourceId(service.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("departmentId", service.getDepartment().getId()));
        hospitalServiceRepository.delete(service);
    }

    private void publishHospitalService(String action, HospitalService service) {
        auditEventPublisher.publish(
                action,
                "HOSPITAL_SERVICE",
                AuditMetadata.resourceId(service.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("departmentId", service.getDepartment().getId()));
    }

    HospitalService find(Long id) {
        return hospitalServiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service hospitalier introuvable : " + id));
    }

    private static int resolveBedsPerRoom(Integer bedsPerRoom) {
        int value = bedsPerRoom == null || bedsPerRoom < 1 ? 1 : bedsPerRoom;
        if (value > 99) {
            throw new BadRequestException("Maximum 99 lits par chambre");
        }
        return value;
    }

    private static int roomCountFor(int bedCapacity, int bedsPerRoom) {
        if (bedCapacity <= 0 || bedsPerRoom <= 0) {
            return 0;
        }
        return (bedCapacity + bedsPerRoom - 1) / bedsPerRoom;
    }

    private HospitalServiceResponse toResponse(HospitalService service) {
        Department department = service.getDepartment();
        int bedsPerRoom = service.getBedsPerRoom();
        int bedCapacity = service.getBedCapacity();
        return new HospitalServiceResponse(
                service.getId(),
                department.getId(),
                department.getCode(),
                department.getName(),
                service.getName(),
                bedCapacity,
                bedsPerRoom,
                String.valueOf(service.getRoomLetterPrefix()),
                roomCountFor(bedCapacity, bedsPerRoom),
                bedRepository.countByHospitalServiceId(service.getId()),
                service.getBedAssignmentPolicy().name(),
                service.isActive()
        );
    }
}
