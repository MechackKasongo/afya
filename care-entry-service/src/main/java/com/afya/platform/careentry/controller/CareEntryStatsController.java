package com.afya.platform.careentry.controller;

import com.afya.platform.careentry.dto.PlatformVolumesResponse;
import com.afya.platform.careentry.model.AdmissionStatus;
import com.afya.platform.careentry.model.EmergencyStatus;
import com.afya.platform.careentry.repository.AdmissionRepository;
import com.afya.platform.careentry.repository.EmergencyVisitRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;

@RestController
@RequestMapping("/api/v1/stats")
public class CareEntryStatsController {

    private static final EnumSet<AdmissionStatus> ACTIVE_ADMISSION = EnumSet.of(
            AdmissionStatus.OUVERTE,
            AdmissionStatus.TRANSFEREE
    );

    private static final EnumSet<AdmissionStatus> TRANSFERRED_ADMISSION = EnumSet.of(AdmissionStatus.TRANSFEREE);

    private static final EnumSet<AdmissionStatus> DISCHARGED_ADMISSION = EnumSet.of(AdmissionStatus.SORTIE);

    private static final EnumSet<AdmissionStatus> DECEASED_ADMISSION = EnumSet.of(AdmissionStatus.DECEDE);

    private static final EnumSet<EmergencyStatus> ACTIVE_EMERGENCY = EnumSet.of(
            EmergencyStatus.EN_ATTENTE_TRIAGE,
            EmergencyStatus.EN_COURS,
            EmergencyStatus.ORIENTE
    );

    private final AdmissionRepository admissionRepository;
    private final EmergencyVisitRepository emergencyVisitRepository;

    public CareEntryStatsController(
            AdmissionRepository admissionRepository,
            EmergencyVisitRepository emergencyVisitRepository
    ) {
        this.admissionRepository = admissionRepository;
        this.emergencyVisitRepository = emergencyVisitRepository;
    }

    @GetMapping("/volumes")
    public PlatformVolumesResponse volumes() {
        long activeEmergencies = emergencyVisitRepository.countByStatusIn(ACTIVE_EMERGENCY);
        return new PlatformVolumesResponse(
                admissionRepository.count(),
                admissionRepository.countByStatusIn(ACTIVE_ADMISSION),
                admissionRepository.countByStatusIn(TRANSFERRED_ADMISSION),
                admissionRepository.countByStatusIn(DISCHARGED_ADMISSION),
                admissionRepository.countByStatusIn(DECEASED_ADMISSION),
                emergencyVisitRepository.count(),
                activeEmergencies);
    }
}
