package com.afya.platform.hospital.repository;

import com.afya.platform.hospital.model.BedOccupation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BedOccupationRepository extends JpaRepository<BedOccupation, Long> {

    Optional<BedOccupation> findByBed_IdAndEndedAtIsNull(Long bedId);

    Page<BedOccupation> findByBed_HospitalService_IdOrderByStartedAtDesc(Long hospitalServiceId, Pageable pageable);

    Page<BedOccupation> findByBed_HospitalService_IdAndBed_IdOrderByStartedAtDesc(
            Long hospitalServiceId,
            Long bedId,
            Pageable pageable
    );

    Page<BedOccupation> findByBed_HospitalService_IdAndAdmissionIdOrderByStartedAtDesc(
            Long hospitalServiceId,
            Long admissionId,
            Pageable pageable
    );

    Page<BedOccupation> findByBed_HospitalService_IdAndPatientIdOrderByStartedAtDesc(
            Long hospitalServiceId,
            Long patientId,
            Pageable pageable
    );

    Page<BedOccupation> findByBed_HospitalService_IdAndEndedAtIsNullOrderByStartedAtDesc(
            Long hospitalServiceId,
            Pageable pageable
    );
}
