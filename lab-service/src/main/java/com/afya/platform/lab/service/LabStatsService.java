package com.afya.platform.lab.service;

import com.afya.platform.lab.dto.LabStatsResponse;
import com.afya.platform.lab.model.ExamRequestStatus;
import com.afya.platform.lab.repository.ExamRequestRepository;
import com.afya.platform.lab.repository.ExamResultRepository;
import com.afya.platform.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LabStatsService {

    private final ExamRequestRepository examRequestRepository;
    private final ExamResultRepository examResultRepository;

    public LabStatsService(
            ExamRequestRepository examRequestRepository,
            ExamResultRepository examResultRepository
    ) {
        this.examRequestRepository = examRequestRepository;
        this.examResultRepository = examResultRepository;
    }

    @Transactional(readOnly = true)
    public LabStatsResponse stats(Instant from, Instant to) {
        Instant rangeFrom = from != null ? from : Instant.EPOCH;
        Instant rangeTo = to != null ? to : Instant.now();
        if (rangeFrom.isAfter(rangeTo)) {
            throw new BadRequestException("La borne 'from' doit être antérieure ou égale à 'to'");
        }
        return new LabStatsResponse(
                rangeFrom,
                rangeTo,
                examRequestRepository.countByRequestedAtBetween(rangeFrom, rangeTo),
                examRequestRepository.countByStatusAndRequestedAtBetween(
                        ExamRequestStatus.PENDING, rangeFrom, rangeTo),
                examRequestRepository.countByStatusAndRequestedAtBetween(
                        ExamRequestStatus.SPECIMEN_COLLECTED, rangeFrom, rangeTo),
                examRequestRepository.countByStatusAndRequestedAtBetween(
                        ExamRequestStatus.RESULTS_AVAILABLE, rangeFrom, rangeTo),
                examResultRepository.countAbnormalParametersBetween(rangeFrom, rangeTo),
                false,
                null
        );
    }
}
