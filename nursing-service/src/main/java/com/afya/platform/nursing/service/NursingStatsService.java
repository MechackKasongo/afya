package com.afya.platform.nursing.service;

import com.afya.platform.nursing.dto.NursingStatsResponse;
import com.afya.platform.nursing.model.PrescriptionNotificationStatus;
import com.afya.platform.nursing.repository.PrescriptionNotificationRepository;
import com.afya.platform.nursing.repository.VitalSignAlertRepository;
import com.afya.platform.nursing.repository.VitalSignReadingRepository;
import com.afya.platform.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NursingStatsService {

    private final VitalSignReadingRepository vitalSignReadingRepository;
    private final VitalSignAlertRepository vitalSignAlertRepository;
    private final PrescriptionNotificationRepository prescriptionNotificationRepository;

    public NursingStatsService(
            VitalSignReadingRepository vitalSignReadingRepository,
            VitalSignAlertRepository vitalSignAlertRepository,
            PrescriptionNotificationRepository prescriptionNotificationRepository
    ) {
        this.vitalSignReadingRepository = vitalSignReadingRepository;
        this.vitalSignAlertRepository = vitalSignAlertRepository;
        this.prescriptionNotificationRepository = prescriptionNotificationRepository;
    }

    @Transactional(readOnly = true)
    public NursingStatsResponse stats(Instant from, Instant to) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        if (rangeFrom.isAfter(rangeTo)) {
            throw new BadRequestException("La borne 'from' doit être antérieure ou égale à 'to'");
        }
        return new NursingStatsResponse(
                rangeFrom,
                rangeTo,
                vitalSignReadingRepository.countByRecordedAtBetween(rangeFrom, rangeTo),
                vitalSignAlertRepository.countByAlertAtBetween(rangeFrom, rangeTo),
                prescriptionNotificationRepository.countBySentAtBetween(rangeFrom, rangeTo),
                prescriptionNotificationRepository.countByStatusAndExecutedAtBetween(
                        PrescriptionNotificationStatus.EXECUTEE, rangeFrom, rangeTo),
                false,
                null
        );
    }
}
