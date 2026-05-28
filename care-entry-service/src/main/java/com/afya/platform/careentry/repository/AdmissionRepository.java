package com.afya.platform.careentry.repository;

import com.afya.platform.careentry.model.Admission;
import com.afya.platform.careentry.model.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    Page<Admission> findAllByOrderByAdmittedAtDesc(Pageable pageable);

    Page<Admission> findByPatientIdOrderByAdmittedAtDesc(Long patientId, Pageable pageable);

    Page<Admission> findByStatusInOrderByAdmittedAtDesc(Collection<AdmissionStatus> statuses, Pageable pageable);

    Page<Admission> findByHospitalServiceIdInOrderByAdmittedAtDesc(
            Collection<Long> hospitalServiceIds,
            Pageable pageable
    );

    Page<Admission> findByPatientIdAndStatusInOrderByAdmittedAtDesc(
            Long patientId,
            Collection<AdmissionStatus> statuses,
            Pageable pageable
    );

    Page<Admission> findByPatientIdAndHospitalServiceIdInOrderByAdmittedAtDesc(
            Long patientId,
            Collection<Long> hospitalServiceIds,
            Pageable pageable
    );

    Page<Admission> findByStatusInAndHospitalServiceIdInOrderByAdmittedAtDesc(
            Collection<AdmissionStatus> statuses,
            Collection<Long> hospitalServiceIds,
            Pageable pageable
    );

    Page<Admission> findByPatientIdAndStatusInAndHospitalServiceIdInOrderByAdmittedAtDesc(
            Long patientId,
            Collection<AdmissionStatus> statuses,
            Collection<Long> hospitalServiceIds,
            Pageable pageable
    );

    long countByStatusIn(Collection<AdmissionStatus> statuses);

    boolean existsByPatientIdAndStatusIn(Long patientId, Collection<AdmissionStatus> statuses);

    Optional<Admission> findFirstByPatientIdAndStatusInOrderByAdmittedAtDesc(
            Long patientId,
            Collection<AdmissionStatus> statuses
    );
}
