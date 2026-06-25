package com.afya.platform.lab.repository;

import com.afya.platform.lab.model.ExamRequest;
import com.afya.platform.lab.model.ExamRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRequestRepository extends JpaRepository<ExamRequest, Long> {

    Page<ExamRequest> findByStatus(ExamRequestStatus status, Pageable pageable);

    Page<ExamRequest> findByPatientId(Long patientId, Pageable pageable);
}
