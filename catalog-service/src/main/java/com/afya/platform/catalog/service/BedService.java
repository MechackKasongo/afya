package com.afya.platform.catalog.service;

import com.afya.platform.shared.audit.AuditActorResolver;
import com.afya.platform.shared.audit.AuditEventPublisher;
import com.afya.platform.shared.audit.AuditMetadata;
import com.afya.platform.catalog.dto.BedOccupancyRequest;
import com.afya.platform.catalog.dto.BedRequest;
import com.afya.platform.catalog.dto.BedResponse;
import com.afya.platform.catalog.support.BedLabelSupport;
import com.afya.platform.catalog.support.BedProvisioningPlanner;
import com.afya.platform.catalog.model.Bed;
import com.afya.platform.catalog.model.HospitalService;
import com.afya.platform.catalog.repository.BedRepository;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BedService {

    private final BedRepository bedRepository;
    private final HospitalServiceCatalogService hospitalServiceCatalogService;
    private final AuditEventPublisher auditEventPublisher;

    public BedService(
            BedRepository bedRepository,
            HospitalServiceCatalogService hospitalServiceCatalogService,
            AuditEventPublisher auditEventPublisher
    ) {
        this.bedRepository = bedRepository;
        this.hospitalServiceCatalogService = hospitalServiceCatalogService;
        this.auditEventPublisher = auditEventPublisher;
    }

    public List<BedResponse> listByService(Long hospitalServiceId) {
        hospitalServiceCatalogService.find(hospitalServiceId);
        return bedRepository.findByHospitalServiceIdOrderByLabelAsc(hospitalServiceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BedResponse create(Long hospitalServiceId, BedRequest request) {
        HospitalService service = hospitalServiceCatalogService.find(hospitalServiceId);
        String label = request.label().strip();
        if (bedRepository.findByHospitalServiceIdAndLabelIgnoreCase(hospitalServiceId, label).isPresent()) {
            throw new ConflictException("Lit déjà défini pour ce service : " + label);
        }
        long count = bedRepository.countByHospitalServiceId(hospitalServiceId);
        if (count >= service.getBedCapacity()) {
            throw new ConflictException("Capacité lit atteinte pour ce service");
        }
        Bed bed = new Bed();
        bed.setHospitalService(service);
        bed.setLabel(label);
        bed.setOccupied(false);
        bed.setLastFreedAt(Instant.now());
        Bed saved = bedRepository.save(bed);
        auditEventPublisher.publish(
                "BED_CREATED",
                "BED",
                AuditMetadata.resourceId(saved.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("hospitalServiceId", hospitalServiceId));
        return toResponse(saved);
    }

    /**
     * Crée chaque lit manquant jusqu'à {@code bed_capacity} (ex. 10 lits, 2/chambre → 001-A, 001-B, 002-A…).
     */
    @Transactional
    public int provisionMissingBeds(Long hospitalServiceId) {
        HospitalService service = hospitalServiceCatalogService.find(hospitalServiceId);
        int capacity = service.getBedCapacity();
        if (capacity <= 0 || bedRepository.countByHospitalServiceId(hospitalServiceId) >= capacity) {
            return 0;
        }
        int created = 0;
        for (String label : BedProvisioningPlanner.plannedLabels(
                capacity, service.getBedsPerRoom(), service.getRoomLetterPrefix())) {
            if (bedRepository.countByHospitalServiceId(hospitalServiceId) >= capacity) {
                break;
            }
            if (bedRepository.findByHospitalServiceIdAndLabelIgnoreCase(hospitalServiceId, label).isPresent()) {
                continue;
            }
            Bed bed = new Bed();
            bed.setHospitalService(service);
            bed.setLabel(label);
            bed.setOccupied(false);
            bed.setLastFreedAt(Instant.now());
            bedRepository.save(bed);
            created++;
        }
        return created;
    }

    /**
     * Supprime les lits libres puis recrée selon la config actuelle (lettre chambre, lits/chambre).
     * Les lits occupés sont conservés ; utile après changement de format (ex. 232-A → A1-01).
     */
    @Transactional
    public int realignFreeBeds(Long hospitalServiceId) {
        hospitalServiceCatalogService.find(hospitalServiceId);
        bedRepository.findByHospitalServiceIdOrderByLabelAsc(hospitalServiceId).stream()
                .filter(bed -> !bed.isOccupied())
                .forEach(bedRepository::delete);
        return provisionMissingBeds(hospitalServiceId);
    }

    @Transactional
    public void updateOccupancy(Long hospitalServiceId, BedOccupancyRequest request) {
        String label = BedLabelSupport.toLabel(request.roomLabel(), request.bedLabel());
        if (label == null) {
            return;
        }
        Bed bed = bedRepository.findByHospitalServiceIdAndLabelIgnoreCase(hospitalServiceId, label)
                .orElseThrow(() -> new NotFoundException("Lit introuvable : " + label));
        if (Boolean.TRUE.equals(request.occupied()) && bed.isOccupied()) {
            throw new ConflictException("Le lit est déjà occupé : " + label);
        }
        if (Boolean.FALSE.equals(request.occupied()) && !bed.isOccupied()) {
            return;
        }
        bed.setOccupied(request.occupied());
        if (Boolean.FALSE.equals(request.occupied())) {
            bed.setLastFreedAt(Instant.now());
        }
        bedRepository.save(bed);
    }

    @Transactional
    public void delete(Long hospitalServiceId, Long bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new NotFoundException("Lit introuvable : " + bedId));
        if (!bed.getHospitalService().getId().equals(hospitalServiceId)) {
            throw new NotFoundException("Lit introuvable pour ce service");
        }
        if (bed.isOccupied()) {
            throw new ConflictException("Impossible de supprimer un lit occupé");
        }
        auditEventPublisher.publish(
                "BED_DELETED",
                "BED",
                AuditMetadata.resourceId(bed.getId()),
                AuditActorResolver.currentUsername(),
                AuditMetadata.json("hospitalServiceId", hospitalServiceId));
        bedRepository.delete(bed);
    }

    private BedResponse toResponse(Bed bed) {
        return new BedResponse(
                bed.getId(),
                bed.getHospitalService().getId(),
                bed.getLabel(),
                bed.isOccupied()
        );
    }
}
