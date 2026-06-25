package com.afya.platform.hospital.service;

import com.afya.platform.hospital.dto.BedOccupationResponse;
import com.afya.platform.hospital.model.Bed;
import com.afya.platform.hospital.model.BedOccupation;
import com.afya.platform.hospital.repository.BedOccupationRepository;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BedOccupationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BedOccupationRepository bedOccupationRepository;
    private final HospitalServiceCatalogService hospitalServiceCatalogService;

    public BedOccupationService(
            BedOccupationRepository bedOccupationRepository,
            HospitalServiceCatalogService hospitalServiceCatalogService
    ) {
        this.bedOccupationRepository = bedOccupationRepository;
        this.hospitalServiceCatalogService = hospitalServiceCatalogService;
    }

    @Transactional(readOnly = true)
    public List<BedOccupationResponse> list(
            Long hospitalServiceId,
            Long bedId,
            Long admissionId,
            Long patientId,
            Boolean activeOnly,
            Integer page,
            Integer size
    ) {
        hospitalServiceCatalogService.find(hospitalServiceId);
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "startedAt"));

        Page<BedOccupation> occupations;
        if (Boolean.TRUE.equals(activeOnly)) {
            occupations = bedOccupationRepository.findByBed_HospitalService_IdAndEndedAtIsNullOrderByStartedAtDesc(
                    hospitalServiceId,
                    pageable
            );
        } else if (bedId != null) {
            occupations = bedOccupationRepository.findByBed_HospitalService_IdAndBed_IdOrderByStartedAtDesc(
                    hospitalServiceId,
                    bedId,
                    pageable
            );
        } else if (admissionId != null) {
            occupations = bedOccupationRepository.findByBed_HospitalService_IdAndAdmissionIdOrderByStartedAtDesc(
                    hospitalServiceId,
                    admissionId,
                    pageable
            );
        } else if (patientId != null) {
            occupations = bedOccupationRepository.findByBed_HospitalService_IdAndPatientIdOrderByStartedAtDesc(
                    hospitalServiceId,
                    patientId,
                    pageable
            );
        } else {
            occupations = bedOccupationRepository.findByBed_HospitalService_IdOrderByStartedAtDesc(
                    hospitalServiceId,
                    pageable
            );
        }

        return occupations.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void recordStart(Bed bed, Long patientId, Long admissionId, Instant startedAt) {
        if (patientId == null || admissionId == null) {
            return;
        }
        bedOccupationRepository.findByBed_IdAndEndedAtIsNull(bed.getId()).ifPresent(open -> {
            throw new ConflictException("Une occupation est déjà ouverte pour le lit " + bed.getLabel());
        });
        BedOccupation occupation = new BedOccupation();
        occupation.setBed(bed);
        occupation.setPatientId(patientId);
        occupation.setAdmissionId(admissionId);
        occupation.setStartedAt(startedAt != null ? startedAt : Instant.now());
        bedOccupationRepository.save(occupation);
    }

    @Transactional
    public void recordEnd(Bed bed, Instant endedAt) {
        bedOccupationRepository.findByBed_IdAndEndedAtIsNull(bed.getId()).ifPresent(open -> {
            open.setEndedAt(endedAt != null ? endedAt : Instant.now());
            bedOccupationRepository.save(open);
        });
    }

    @Transactional(readOnly = true)
    public BedOccupationResponse getById(Long hospitalServiceId, Long occupationId) {
        hospitalServiceCatalogService.find(hospitalServiceId);
        BedOccupation occupation = bedOccupationRepository.findById(occupationId)
                .orElseThrow(() -> new NotFoundException("Occupation introuvable : " + occupationId));
        if (!occupation.getBed().getHospitalService().getId().equals(hospitalServiceId)) {
            throw new NotFoundException("Occupation introuvable pour ce service");
        }
        return toResponse(occupation);
    }

    private BedOccupationResponse toResponse(BedOccupation occupation) {
        Bed bed = occupation.getBed();
        return new BedOccupationResponse(
                occupation.getId(),
                bed.getId(),
                bed.getLabel(),
                occupation.getPatientId(),
                occupation.getAdmissionId(),
                occupation.getStartedAt(),
                occupation.getEndedAt()
        );
    }
}
