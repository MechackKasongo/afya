package com.afya.platform.admission.stay.repository;

import com.afya.platform.admission.stay.model.Stay;
import com.afya.platform.admission.stay.model.StayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface StayRepository extends JpaRepository<Stay, Long> {

    Optional<Stay> findByAdmissionId(Long admissionId);

    boolean existsByAdmissionId(Long admissionId);

    Page<Stay> findByPatientIdOrderByCheckInAtDesc(Long patientId, Pageable pageable);

    boolean existsByPatientIdAndStatusIn(Long patientId, Collection<StayStatus> statuses);

    long countByStatus(StayStatus status);
}
