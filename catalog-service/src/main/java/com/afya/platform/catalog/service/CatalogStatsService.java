package com.afya.platform.catalog.service;

import com.afya.platform.catalog.dto.BedSuggestionResponse;
import com.afya.platform.catalog.dto.OccupancyStatsResponse;
import com.afya.platform.catalog.dto.ServiceOccupancyStats;
import com.afya.platform.catalog.model.HospitalService;
import com.afya.platform.catalog.repository.BedRepository;
import com.afya.platform.catalog.repository.HospitalServiceRepository;
import com.afya.platform.catalog.support.BedLabelSupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogStatsService {

    private final BedRepository bedRepository;
    private final HospitalServiceRepository hospitalServiceRepository;
    private final BedService bedService;
    private final BedAssignmentSelector bedAssignmentSelector;

    public CatalogStatsService(
            BedRepository bedRepository,
            HospitalServiceRepository hospitalServiceRepository,
            BedService bedService,
            BedAssignmentSelector bedAssignmentSelector
    ) {
        this.bedRepository = bedRepository;
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.bedService = bedService;
        this.bedAssignmentSelector = bedAssignmentSelector;
    }

    public OccupancyStatsResponse occupancy() {
        long totalBeds = bedRepository.count();
        long occupiedBeds = bedRepository.countByOccupiedTrue();
        long availableBeds = Math.max(0, totalBeds - occupiedBeds);
        double rate = totalBeds == 0 ? 0.0 : (occupiedBeds * 100.0) / totalBeds;
        List<ServiceOccupancyStats> byService = occupancyByService();
        return new OccupancyStatsResponse(rate, totalBeds, occupiedBeds, availableBeds, byService);
    }

    private List<ServiceOccupancyStats> occupancyByService() {
        List<ServiceOccupancyStats> rows = new ArrayList<>();
        for (HospitalService service : hospitalServiceRepository.findByActiveTrueOrderByNameAsc()) {
            long serviceTotal = bedRepository.countByHospitalServiceId(service.getId());
            if (serviceTotal == 0) {
                continue;
            }
            long serviceOccupied = bedRepository.countByHospitalServiceIdAndOccupiedTrue(service.getId());
            long serviceAvailable = Math.max(0, serviceTotal - serviceOccupied);
            double serviceRate = (serviceOccupied * 100.0) / serviceTotal;
            rows.add(new ServiceOccupancyStats(
                    service.getId(),
                    service.getName(),
                    service.getDepartment().getName(),
                    serviceTotal,
                    serviceOccupied,
                    serviceAvailable,
                    serviceRate));
        }
        return rows;
    }

    public BedSuggestionResponse suggestBed(Long hospitalServiceId) {
        HospitalService service = hospitalServiceRepository.findById(hospitalServiceId)
                .orElseThrow(() -> new com.afya.platform.shared.exception.NotFoundException(
                        "Service hospitalier introuvable : " + hospitalServiceId));
        int capacity = service.getBedCapacity();
        if (capacity <= 0) {
            return new BedSuggestionResponse(false, null, null, 0, 0, null);
        }
        long occupied = bedRepository.countByHospitalServiceIdAndOccupiedTrue(hospitalServiceId);
        if (bedRepository.countByHospitalServiceId(hospitalServiceId) < capacity) {
            bedService.provisionMissingBeds(hospitalServiceId);
        }
        List<com.afya.platform.catalog.model.Bed> freeBeds =
                bedRepository.findByHospitalServiceIdAndOccupiedFalse(hospitalServiceId);
        return bedAssignmentSelector.pickFreeBed(service, freeBeds)
                .map(bed -> {
                    BedLabelSupport.Parsed parsed = BedLabelSupport.parse(bed.getLabel());
                    return new BedSuggestionResponse(
                            true,
                            parsed.room(),
                            parsed.bed(),
                            occupied,
                            capacity,
                            null);
                })
                .orElseGet(() -> new BedSuggestionResponse(
                        false,
                        null,
                        null,
                        occupied,
                        capacity,
                        "Aucun lit libre pour le service « " + service.getName() + " »"));
    }
}
