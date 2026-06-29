package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.ExamRequest;
import com.afya.platform.lab.model.ExamRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExamRequestRepository
        extends JpaRepository<ExamRequest, Long>, JpaSpecificationExecutor<ExamRequest> {

    Page<ExamRequest> findByStatus(ExamRequestStatus status, Pageable pageable);

    Page<ExamRequest> findByDoctorId(Long doctorId, Pageable pageable);

    Page<ExamRequest> findByDoctorIdAndStatus(Long doctorId, ExamRequestStatus status, Pageable pageable);

    long countByDoctorIdAndStatus(Long doctorId, ExamRequestStatus status);

    Page<ExamRequest> findByPatientId(Long patientId, Pageable pageable);

    long countByRequestedAtBetween(java.time.Instant from, java.time.Instant to);

    long countByStatusAndRequestedAtBetween(
            ExamRequestStatus status,
            java.time.Instant from,
            java.time.Instant to
    );
}
